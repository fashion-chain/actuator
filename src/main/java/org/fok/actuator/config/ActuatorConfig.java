package org.fok.actuator.config;

import com.google.protobuf.Message;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.ntrans.api.ActorService;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import onight.tfw.outils.conf.PropHelper;

import java.math.BigInteger;

@Slf4j
public class ActuatorConfig {
	private static PropHelper prop = new PropHelper(null);
	
	// 手续费存储账户
	public static String lock_account_address = prop.get("org.brewchain.account.lock.address", null);
	// token创建时的手续费
	public static BigInteger token_create_lock_balance = readBigIntegerValue("org.brewchain.token.create.lock.balance", "0");
	// token增发时的手续费
	public static BigInteger token_mint_lock_balance = readBigIntegerValue("org.brewchain.token.mint.lock.balance", "0");
	// token燃烧时的手续费
	public static BigInteger token_burn_lock_balance = readBigIntegerValue("org.brewchain.token.burn.lock.balance", "0");
	// token冻结的手续费
	public static BigInteger token_freeze_lock_balance = readBigIntegerValue("org.brewchain.token.freeze.lock.balance", "0");
	// token解冻的手续费
	public static BigInteger token_unfreeze_lock_balance = readBigIntegerValue("org.brewchain.token.unfreeze.lock.balance", "0");
	// 合约创建时的手续费
	public static BigInteger contract_create_lock_balance = readBigIntegerValue("org.brewchain.contract.lock.balance", "0");
	// cryptotoken创建时的手续费
	public static BigInteger crypto_token_create_lock_balance = readBigIntegerValue("org.brewchain.cryptotoken.lock.balance", "0");
	// token的最小发行数量
	public static BigInteger minTokenTotal = readBigIntegerValue("org.brewchain.token.min.total", "0");
	// token的最大发行数量
	public static BigInteger maxTokenTotal = readBigIntegerValue("org.brewchain.token.max.total", "0");
	// 一个区块的奖励
	public static BigInteger minerReward = new BigInteger(prop.get("block.miner.reward", "0"));
	
	private static BigInteger readBigIntegerValue(String key, String defaultVal) {
		try {
			return new BigInteger(prop.get(key, defaultVal));
		} catch (Exception e) {
			log.error("cannot read key::" + key, e);
		}
		return new BigInteger(defaultVal);
	}

}
