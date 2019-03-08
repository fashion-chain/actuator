package org.fok.actuator.enums;

/**
 * Email: king.camulos@gmail.com
 * Date: 2018/11/7
 * DESC:
 */
public enum ActuatorTypeEnum {

    TYPE_DEFAULT(0),
    TYPE_CreateUnionAccount(1),
    TYPE_TokenTransaction(2),
    TYPE_UnionAccountTransaction(3),
    // TYPE_CallInternalFunction(4),
    TYPE_CryptoTokenTransaction(5),
    TYPE_LockTokenTransaction(6),
    TYPE_CreateContract(7),
    TYPE_CallContract(8),
    TYPE_CreateToken(9),
    TYPE_CreateCryptoToken(10),
    TYPE_Sanction(11),
    TYPE_UnionAccountTokenTransaction(12),
    TYPE_MintToken(13),
    TYPE_BurnToken(14),
    TYPE_freezeToken(15),
    TYPE_unfreezeToken(16),
    TYPE_CoinBase(888);

    private int value = 0;

    private ActuatorTypeEnum(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static ActuatorTypeEnum transf(int t) {
        for (ActuatorTypeEnum type : values()) {
            if (type.value == t) {
                return type;
            }
        }
        return TYPE_DEFAULT;
    }
}
