package org.fok.actuator.evmapi;

import com.google.protobuf.ByteString;
import lombok.Data;
import org.fok.core.api.IAccountEVMHandler;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class AccountEVMHandler implements IAccountEVMHandler {
	IAccountHandler accountHandler;
	ITransactionHandler transactionHandler;
	ICryptoHandler cryptoHandler;
	BytesHashMap<AccountInfo.Builder> touchAccount;

	private AccountInfo.Builder getAccount(ByteString addr) {
		if (!touchAccount.containsKey(addr.toByteArray())) {
			return null;
		} else {
			return touchAccount.get(addr.toByteArray());
		}
	}

	@Override
	public ICryptoHandler getCrypto() {
		return cryptoHandler;
	}

	@Override
	public AccountInfo GetAccount(ByteString addr) {
		return getAccount(addr).build();
	}

	@Override
	public BigInteger addBalance(ByteString addr, BigInteger balance) {
		try {
			AccountInfo.Builder oAccount = getAccount(addr);
			if (oAccount == null) {

			} else {
				accountHandler.addBalance(oAccount, balance);
			}
			touchAccount.put(addr.toByteArray(), oAccount);
			return BytesHelper.bytesToBigInteger(oAccount.getValue().getBalance().toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return BigInteger.ZERO;
	}

	@Override
	public BigInteger getBalance(ByteString addr) {
		try {
			AccountInfo.Builder oAccount = getAccount(addr);
			return this.accountHandler.getBalance(oAccount);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return BigInteger.ZERO;
	}

	@Override
	public boolean isExist(ByteString addr) {
		try {
			return this.touchAccount.containsKey(addr.toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void saveStorage(ByteString address, byte[] key, byte[] value) {
		AccountInfo.Builder oAccount = getAccount(address);
		this.accountHandler.putAccountStorage(oAccount, key, value);
	}

	@Override
	public Map<String, byte[]> getStorage(ByteString address, List<byte[]> keys) {
		AccountInfo.Builder oAccount = getAccount(address);
		Map<String, byte[]> storageValue = new HashMap<>();
		for (byte[] key : keys) {
			storageValue.put(this.cryptoHandler.bytesToHexStr(key),
					this.accountHandler.getAccountStorage(oAccount, key));
		}
		return storageValue;
	}

	@Override
	public byte[] getStorage(ByteString address, byte[] key) {
		AccountInfo.Builder oAccount = getAccount(address);
		return this.accountHandler.getAccountStorage(oAccount, key);
	}

}
