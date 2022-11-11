package cn.itcast.account.service;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * @author liuhangfei
 * @create 2022-11-11 17:12
 * @description 自定义接口 实现TTC模式
 */
@LocalTCC
public interface AccountTccService {

    /**
     * 用户余额扣减
     * @param userId 用户ID
     * @param money 用户余额
     */
    @TwoPhaseBusinessAction(name = "deduct", commitMethod = "confirm", rollbackMethod = "cancel")
    void deduct(@BusinessActionContextParameter(paramName = "userId") String userId,
                @BusinessActionContextParameter(paramName = "userId") int money);

    /**
     * 提交事务
     * @param context
     */
    boolean confirm(BusinessActionContext context);

    /**
     * 回滚事务
     * @param context
     */
    boolean cancel(BusinessActionContext context);

}
