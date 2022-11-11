package cn.itcast.account.service.impl;

import cn.itcast.account.entity.AccountFreeze;
import cn.itcast.account.mapper.AccountFreezeMapper;
import cn.itcast.account.mapper.AccountMapper;
import cn.itcast.account.service.AccountTccService;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author liuhangfei
 * @create 2022-11-11 17:23
 * @description
 */
@Service
public class AccountTccServiceImpl implements AccountTccService {

    @Resource
    private AccountMapper accountMapper;
    @Resource
    private AccountFreezeMapper accountFreezeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(String userId, int money) {
        // 获取事务ID
        String xid = RootContext.getXID();

        // 判断freeze 中是否有冻结记录 做悬挂判断
        AccountFreeze freeze1 = accountFreezeMapper.selectById(xid);
        if(freeze1 == null){
            // 扣减可用余额
            accountMapper.deduct(userId, money);
            // 记录冻结金额 事务状态
            AccountFreeze freeze = new AccountFreeze();
            freeze.setXid(xid);
            freeze.setUserId(userId);
            freeze.setFreezeMoney(money);
            freeze.setState(AccountFreeze.State.TRY);
            accountFreezeMapper.insert(freeze);
        }
    }

    @Override
    public boolean confirm(BusinessActionContext context) {
        // 获取事务ID
        String xid = context.getXid();
        // 删除冻结金额记录
        int delete = accountFreezeMapper.deleteById(xid);
        return delete == 1;
    }

    @Override
    public boolean cancel(BusinessActionContext context) {
        // 查询冻结记录
        String xid = context.getXid();
        AccountFreeze accountFreeze = accountFreezeMapper.selectById(xid);
        // 判断accountFreeze 为null 证明try没执行 空回滚
        if (accountFreeze == null){
            AccountFreeze freeze = new AccountFreeze();
            freeze.setXid(xid);
            freeze.setUserId(context.getActionContext("userId").toString());
            freeze.setFreezeMoney(0);
            freeze.setState(AccountFreeze.State.CANCEL);
            int insert = accountFreezeMapper.insert(freeze);
            return insert == 1;
        }
        // 幂等校验
        if (accountFreeze.getState().equals(AccountFreeze.State.CANCEL)){
            return true;
        }
        // 恢复可用余额
        accountMapper.refund(accountFreeze.getUserId(), accountFreeze.getFreezeMoney());
        // 冻结金额清零 事务状态
        AccountFreeze freeze = new AccountFreeze();
        freeze.setFreezeMoney(0);
        freeze.setState(AccountFreeze.State.CANCEL);
        int update = accountFreezeMapper.updateById(freeze);
        return update == 1;
    }
}
