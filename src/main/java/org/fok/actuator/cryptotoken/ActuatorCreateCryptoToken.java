package org.fok.actuator.cryptotoken;

import com.google.protobuf.ByteString;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Account.AccountValue;
import org.fok.core.model.Account.CryptoTokenOriginTransactionValue;
import org.fok.core.model.Account.CryptoTokenOriginValue;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountCryptoToken;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.CryptoTokenData;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.tools.bytes.BytesComparisons;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;
import org.fok.actuator.AbstractTransactionActuator;
import org.fok.actuator.config.ActuatorConfig;
import org.fok.actuator.exception.TransactionParameterInvalidException;

import java.util.ArrayList;
import java.util.List;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/7 DESC:
 */
public class ActuatorCreateCryptoToken extends AbstractTransactionActuator {
	public ActuatorCreateCryptoToken(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler,
			BlockInfo blockInfo, ICryptoHandler iCryptoHandler) {
		super(iAccountHandler, iTransactionHandler, blockInfo, iCryptoHandler);
	}

	@Override
	public void onPrepareExecute(TransactionInfo oMultiTransaction, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {

		if (oMultiTransaction.getBody().getInputs() == null) {
			throw new TransactionParameterInvalidException("parameter invalid, inputs must be only one");
		}

		CryptoTokenData oCryptoTokenData = CryptoTokenData
				.parseFrom(oMultiTransaction.getBody().getData().toByteArray());
		if (oCryptoTokenData.getCodeCount() != oCryptoTokenData.getNameCount() || oCryptoTokenData.getCodeCount() == 0
				|| oCryptoTokenData.getTotal() == 0) {
			throw new TransactionParameterInvalidException("parameter invalid, crypto token count must large than 0");
		}

		if (oCryptoTokenData.getSymbol() == null || oCryptoTokenData.getSymbol().isEmpty()) {
			throw new TransactionParameterInvalidException("parameter invalid, crypto token symbol must not be null");
		}

		AccountInfo.Builder cryptoRecordAccount = accounts.get(iAccountHandler.cryptoTokenValueAddress());
		CryptoTokenOriginValue oCryptoTokenOriginValue = iAccountHandler.getCryptoTokenOrigin(cryptoRecordAccount,
				oCryptoTokenData.getSymbol().toByteArray());

		if (oCryptoTokenOriginValue == null) {
			throw new TransactionParameterInvalidException("parameter invalid, token record account not found");
		} else if (oCryptoTokenOriginValue.getTotal() < oCryptoTokenOriginValue.getCurrent()
				+ oCryptoTokenData.getCodeCount()) {
			throw new TransactionParameterInvalidException("parameter invalid, cannot create crypto token with name "
					+ oMultiTransaction.getBody().getInputs().getSymbol());
		} else if (!BytesComparisons.equal(oCryptoTokenOriginValue.getOwner().toByteArray(),
				oMultiTransaction.getBody().getInputs().getAddress().toByteArray())) {
			throw new TransactionParameterInvalidException("parameter invalid, cannot create crypto token with name "
					+ oMultiTransaction.getBody().getInputs().getSymbol());
		} else if (oCryptoTokenData.getSymbol().toByteArray().length > 32) {
			throw new TransactionParameterInvalidException("parameter invalid, crypto token symbol too long");
		} else if (!BytesComparisons.equal(oCryptoTokenOriginValue.getOwner().toByteArray(),
				oMultiTransaction.getBody().getInputs().getAddress().toByteArray())) {
			throw new TransactionParameterInvalidException("parameter invalid, cannot create crypto token with address "
					+ iCryptoHandler.bytesToHexStr(oMultiTransaction.getBody().getInputs().getAddress().toByteArray()));
		} else if (oCryptoTokenData.getTotal() != oCryptoTokenOriginValue.getTotal()) {
			throw new TransactionParameterInvalidException("parameter invalid, transaction data invalid");
		}

		if (oCryptoTokenOriginValue == null || oCryptoTokenOriginValue.getTxHashCount() == 0) {
			// 只有第一次创建，需要扣除手续费
			AccountInfo.Builder sender = accounts
					.get(oMultiTransaction.getBody().getInputs().getAddress().toByteArray());
			AccountValue.Builder senderValue = sender.getValueBuilder();
			if (BytesHelper.bytesToBigInteger(senderValue.getBalance().toByteArray())
					.compareTo(ActuatorConfig.crypto_token_create_lock_balance) < 0) {
				throw new TransactionParameterInvalidException(
						"parameter invalid, donot have enouth balance to create crypto-token");
			}
		} else {
			// 取最后一次的token信息
			ByteString originTxHash = oCryptoTokenOriginValue.getTxHash(oCryptoTokenOriginValue.getTxHashCount() - 1);
			byte[] originValueByte = this.iAccountHandler.getAccountStorage(cryptoRecordAccount,
					originTxHash.toByteArray());

			CryptoTokenOriginTransactionValue oCryptoTokenOriginTransactionValue = CryptoTokenOriginTransactionValue
					.parseFrom(originValueByte);

			if ((oCryptoTokenOriginTransactionValue.getEndIndex()
					+ oCryptoTokenData.getNameCount()) > oCryptoTokenOriginValue.getTotal()) {
				throw new TransactionParameterInvalidException("parameter invalid, too many crypto token");
			}
		}

		super.onPrepareExecute(oMultiTransaction, accounts);
	}

	@Override
	public ByteString onExecute(TransactionInfo oMultiTransaction, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {

		CryptoTokenData oCryptoTokenData = CryptoTokenData
				.parseFrom(oMultiTransaction.getBody().getData().toByteArray());

		TransactionInput input = oMultiTransaction.getBody().getInputs();
		AccountInfo.Builder sender = accounts.get(input.getAddress().toByteArray());
		iAccountHandler.setNonce(sender, input.getNonce() + 1);

		AccountInfo.Builder cryptoRecordAccount = accounts.get(iAccountHandler.cryptoTokenValueAddress());
		CryptoTokenOriginValue.Builder oCryptoTokenOriginValue = iAccountHandler
				.getCryptoTokenOrigin(cryptoRecordAccount, oCryptoTokenData.getSymbol().toByteArray()).toBuilder();

		AccountInfo.Builder cryptoAccount = this.iAccountHandler
				.createAccount(this.iCryptoHandler.sha3(oCryptoTokenData.getSymbol().toByteArray()));

		List<AccountCryptoToken> tokens = new ArrayList<>();
		for (int i = 0; i < oCryptoTokenData.getNameCount(); i++) {
			AccountCryptoToken.Builder oAccountCryptoToken = AccountCryptoToken.newBuilder();
			oAccountCryptoToken.setCode(oCryptoTokenData.getCode(i));
			oAccountCryptoToken.setExtData(oCryptoTokenData.getExtData());
			oAccountCryptoToken.setIndex(oCryptoTokenOriginValue.getCurrent() + i + 1);
			oAccountCryptoToken.setName(oCryptoTokenData.getName(i));
			oAccountCryptoToken.setTotal(oCryptoTokenOriginValue.getTotal());
			oAccountCryptoToken.setTimestamp(oMultiTransaction.getBody().getTimestamp());
			oAccountCryptoToken.clearHash();
			oAccountCryptoToken.setHash(
					ByteString.copyFrom(this.iCryptoHandler.sha256(oAccountCryptoToken.build().toByteArray())));
			oAccountCryptoToken.setNonce(0);
			oAccountCryptoToken.setOwner(input.getAddress());
			oAccountCryptoToken.setOwnertime(oMultiTransaction.getBody().getTimestamp());

			oCryptoTokenOriginValue.setCurrent(oCryptoTokenOriginValue.getCurrent() + 1);
			tokens.add(oAccountCryptoToken.build());
			this.iAccountHandler.addCryptoTokenBalance(sender, oCryptoTokenData.getSymbol().toByteArray(),
					oAccountCryptoToken.build());

			this.iAccountHandler.putAccountStorage(cryptoAccount, oAccountCryptoToken.getHash().toByteArray(),
					oAccountCryptoToken.build().toByteArray());
		}

		oCryptoTokenOriginValue.addTxHash(oMultiTransaction.getHash());

		this.iAccountHandler.putCryptoTokenOrigin(cryptoRecordAccount, oCryptoTokenData.getSymbol().toByteArray(),
				oCryptoTokenOriginValue.build());

		accounts.put(cryptoRecordAccount.getAddress().toByteArray(), cryptoRecordAccount);
		accounts.put(sender.getAddress().toByteArray(), sender);

		return ByteString.EMPTY;
	}
}
