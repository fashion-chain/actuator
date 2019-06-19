package org.fok.actuator.cryptotoken;

import com.google.protobuf.ByteString;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Account.AccountValue;
import org.fok.core.model.Account.CryptoTokenOriginTransactionValue;
import org.fok.core.model.Account.CryptoTokenOriginValue;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionResultHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountCryptoToken;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionData.PublicCryptoTokenData;
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
	public ActuatorCreateCryptoToken(IAccountHandler iAccountHandler, ITransactionResultHandler iTransactionHandler,
			ICryptoHandler iCryptoHandler, BlockInfo blockInfo) {
		super(iAccountHandler, iTransactionHandler, iCryptoHandler, blockInfo);
	}

	@Override
	public void onPrepareExecute(TransactionInfo oMultiTransaction) throws Exception {
		PublicCryptoTokenData oCryptoTokenData = oMultiTransaction.getBody().getData().getCryptoTokenData();
		if (oCryptoTokenData.getCodeCount() != oCryptoTokenData.getNameCount() || oCryptoTokenData.getCodeCount() == 0
				|| oCryptoTokenData.getTotal() == 0) {
			throw new TransactionParameterInvalidException("parameter invalid, crypto token count must large than 0");
		}

		if (oCryptoTokenData.getSymbol() == null || oCryptoTokenData.getSymbol().isEmpty()) {
			throw new TransactionParameterInvalidException("parameter invalid, crypto token symbol must not be null");
		}

		AccountInfo.Builder cryptoRecordAccount = this.getAccount(iAccountHandler.cryptoTokenValueAddress());
		CryptoTokenOriginValue oCryptoTokenOriginValue = iAccountHandler.getCryptoTokenOrigin(cryptoRecordAccount,
				oCryptoTokenData.getSymbol().toByteArray());

		if (oCryptoTokenOriginValue == null) {
			throw new TransactionParameterInvalidException("parameter invalid, token record account not found");
		} else if (oCryptoTokenOriginValue.getTotal() < oCryptoTokenOriginValue.getCurrent()
				+ oCryptoTokenData.getCodeCount()) {
			throw new TransactionParameterInvalidException("parameter invalid, cannot create crypto token with name "
					+ this.iCryptoHandler.bytesToHexStr(oCryptoTokenData.getSymbol().toByteArray()));
		} else if (!BytesComparisons.equal(oCryptoTokenOriginValue.getOwner().toByteArray(),
				oMultiTransaction.getBody().getInput().getAddress().toByteArray())) {
			throw new TransactionParameterInvalidException("parameter invalid, cannot create crypto token with name "
					+ this.iCryptoHandler.bytesToHexStr(oCryptoTokenData.getSymbol().toByteArray()));
		} else if (oCryptoTokenData.getSymbol().toByteArray().length > 32) {
			throw new TransactionParameterInvalidException("parameter invalid, crypto token symbol too long");
		} else if (!BytesComparisons.equal(oCryptoTokenOriginValue.getOwner().toByteArray(),
				oMultiTransaction.getBody().getInput().getAddress().toByteArray())) {
			throw new TransactionParameterInvalidException("parameter invalid, cannot create crypto token with address "
					+ iCryptoHandler.bytesToHexStr(oMultiTransaction.getBody().getInput().getAddress().toByteArray()));
		} else if (oCryptoTokenData.getTotal() != oCryptoTokenOriginValue.getTotal()) {
			throw new TransactionParameterInvalidException("parameter invalid, transaction data invalid");
		}

		if (oCryptoTokenOriginValue == null || oCryptoTokenOriginValue.getTxHashCount() == 0) {
			// 只有第一次创建，需要扣除手续费
			AccountInfo.Builder sender = this
					.getAccount(oMultiTransaction.getBody().getInput().getAddress());
			if (this.iAccountHandler.getBalance(sender)
					.compareTo(ActuatorConfig.crypto_token_create_lock_balance.add(fee)) < 0) {
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

			AccountInfo.Builder sender = this
					.getAccount(oMultiTransaction.getBody().getInput().getAddress());
			if (this.iAccountHandler.getBalance(sender).compareTo(fee) < 0) {
				throw new TransactionParameterInvalidException(
						"parameter invalid, donot have enouth balance to create crypto-token");
			}
		}

		super.onPrepareExecute(oMultiTransaction);
	}

	@Override
	public ByteString onExecute(TransactionInfo oMultiTransaction) throws Exception {
		PublicCryptoTokenData oCryptoTokenData = PublicCryptoTokenData
				.parseFrom(oMultiTransaction.getBody().getData().toByteArray());

		TransactionInput input = oMultiTransaction.getBody().getInput();
		AccountInfo.Builder sender = this.getAccount(input.getAddress());
		AccountInfo.Builder lockAccount = this
				.getAccount(this.iCryptoHandler.hexStrToBytes(ActuatorConfig.lock_account_address));

		iAccountHandler.setNonce(sender, input.getNonce() + 1);

		AccountInfo.Builder cryptoRecordAccount = this.getAccount(iAccountHandler.cryptoTokenValueAddress());
		CryptoTokenOriginValue.Builder oCryptoTokenOriginValue = iAccountHandler
				.getCryptoTokenOrigin(cryptoRecordAccount, oCryptoTokenData.getSymbol().toByteArray()).toBuilder();

		if (oCryptoTokenOriginValue == null || oCryptoTokenOriginValue.getTxHashCount() == 0) {
			oCryptoTokenOriginValue = CryptoTokenOriginValue.newBuilder();
			this.iAccountHandler.subBalance(sender, ActuatorConfig.crypto_token_create_lock_balance.add(fee));
			this.iAccountHandler.addBalance(lockAccount, ActuatorConfig.crypto_token_create_lock_balance.add(fee));
		} else {
			this.iAccountHandler.subBalance(sender, fee);
			this.iAccountHandler.addBalance(lockAccount, fee);
		}

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

		this.putAccount(cryptoRecordAccount);
		this.putAccount(cryptoAccount);
		this.putAccount(sender);
		this.putAccount(lockAccount);

		return ByteString.EMPTY;
	}
}
