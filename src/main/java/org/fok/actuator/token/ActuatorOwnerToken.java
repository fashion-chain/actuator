package org.fok.actuator.token;

import com.google.protobuf.ByteString;
import onight.tfw.ntrans.api.annotation.ActorRequire;

import java.math.BigInteger;

import org.fok.actuator.AbstractTransactionActuator;
import org.fok.actuator.AbstractUnTransferActuator;
import org.fok.actuator.config.ActuatorConfig;
import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionResultHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.*;
import org.fok.core.model.Account.AccountInfo.Builder;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionData.OwnerTokenData;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;
import org.fok.tools.unit.UnitHelper;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/15 DESC:
 */
public class ActuatorOwnerToken extends AbstractUnTransferActuator {

	public ActuatorOwnerToken(IAccountHandler iAccountHandler, ITransactionResultHandler iTransactionHandler,
			ICryptoHandler iCryptoHandler, BlockInfo currentBlock) {
		super(iAccountHandler, iTransactionHandler, iCryptoHandler, currentBlock);
		// TODO Auto-generated constructor stub
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo) throws Exception {
		TransactionInput input = transactionInfo.getBody().getInput();
		AccountInfo.Builder sender = this.getAccount(input.getAddress().toByteArray());
		AccountInfo.Builder lockAccount = this
				.getAccount(this.iCryptoHandler.hexStrToBytes(ActuatorConfig.lock_account_address));
		AccountInfo.Builder tokenAccount = this.getAccount(iAccountHandler.tokenValueAddress());
		OwnerTokenData oOwnerTokenData = transactionInfo.getBody().getData().getOwnerTokenData();

		this.iAccountHandler.setNonce(sender, input.getNonce() + 1);
		this.iAccountHandler.subBalance(sender, fee);

		if (oOwnerTokenData.getOpCode() == OwnerTokenData.OwnerTokenOpCode.PUBLIC) {
			this.iAccountHandler.subBalance(sender, ActuatorConfig.token_create_lock_balance);
			this.iAccountHandler.addTokenBalance(sender, oOwnerTokenData.getToken().toByteArray(),
					BytesHelper.bytesToBigInteger(oOwnerTokenData.getAmount().toByteArray()));

			this.iAccountHandler.addBalance(lockAccount, ActuatorConfig.token_create_lock_balance);

			TokenValue.Builder oTokenValue = TokenValue.newBuilder();
			oTokenValue.setAddress(sender.getAddress());
			oTokenValue.setTimestamp(transactionInfo.getBody().getTimestamp());
			oTokenValue.setToken(oOwnerTokenData.getToken());
			oTokenValue.setTotal(oOwnerTokenData.getAmount());

			TokenValueHistory.Builder oTokenValueHistory = TokenValueHistory.newBuilder();
			oTokenValueHistory.setContent("C");
			oTokenValueHistory.setTotal(oTokenValue.getTotal());
			oTokenValueHistory.setTimestamp(transactionInfo.getBody().getTimestamp());
			oTokenValue.addHistory(oTokenValueHistory);
			this.iAccountHandler.putToken(tokenAccount, oOwnerTokenData.getToken().toByteArray(), oTokenValue.build());

			this.putAccount(lockAccount);
			this.putAccount(tokenAccount);
		} else if (oOwnerTokenData.getOpCode() == OwnerTokenData.OwnerTokenOpCode.BURN) {
			this.iAccountHandler.subBalance(sender, ActuatorConfig.token_burn_lock_balance);
			this.iAccountHandler.subTokenBalance(sender, oOwnerTokenData.getToken().toByteArray(),
					BytesHelper.bytesToBigInteger(oOwnerTokenData.getAmount().toByteArray()));

			TokenValue.Builder oTokenValue = this.iAccountHandler
					.getToken(tokenAccount, oOwnerTokenData.getToken().toByteArray()).toBuilder();
			oTokenValue.setTotal(ByteString.copyFrom(BytesHelper.bigIntegerToBytes(
					BytesHelper.bytesToBigInteger(oTokenValue.getTotal().toByteArray()).subtract(UnitHelper
							.fromWei(BytesHelper.bytesToBigInteger(oOwnerTokenData.getAmount().toByteArray()))))));

			TokenValueHistory.Builder oTokenValueHistory = TokenValueHistory.newBuilder();
			oTokenValueHistory.setContent("B");
			oTokenValueHistory.setTotal(oTokenValue.getTotal());
			oTokenValueHistory.setTimestamp(transactionInfo.getBody().getTimestamp());
			oTokenValue.addHistory(oTokenValueHistory);
			this.iAccountHandler.putToken(tokenAccount, oTokenValue.getToken().toByteArray(), oTokenValue.build());
			this.iAccountHandler.addBalance(lockAccount, ActuatorConfig.token_burn_lock_balance);

			this.putAccount(lockAccount);
			this.putAccount(tokenAccount);
		} else if (oOwnerTokenData.getOpCode() == OwnerTokenData.OwnerTokenOpCode.MINT) {
			this.iAccountHandler.subBalance(sender, ActuatorConfig.token_mint_lock_balance);
			this.iAccountHandler.addTokenBalance(sender, oOwnerTokenData.getToken().toByteArray(),
					BytesHelper.bytesToBigInteger(oOwnerTokenData.getAmount().toByteArray()));

			this.iAccountHandler.addBalance(lockAccount, ActuatorConfig.token_mint_lock_balance);

			TokenValue.Builder oTokenValue = this.iAccountHandler
					.getToken(sender, oOwnerTokenData.getToken().toByteArray()).toBuilder();
			oTokenValue.setTotal(ByteString.copyFrom(BytesHelper.bigIntegerToBytes(
					BytesHelper.bytesToBigInteger(oTokenValue.getTotal().toByteArray()).add(UnitHelper
							.fromWei(BytesHelper.bytesToBigInteger(oOwnerTokenData.getAmount().toByteArray()))))));

			TokenValueHistory.Builder oTokenValueHistory = TokenValueHistory.newBuilder();
			oTokenValueHistory.setContent("C");
			oTokenValueHistory.setTotal(oTokenValue.getTotal());
			oTokenValueHistory.setTimestamp(transactionInfo.getBody().getTimestamp());
			oTokenValue.addHistory(oTokenValueHistory);

			this.putAccount(lockAccount);
			this.putAccount(tokenAccount);
		}

		this.putAccount(sender);
		return ByteString.EMPTY;
	}

	@Override
	public void onPrepareExecute(TransactionInfo transactionInfo) throws Exception {
		super.onPrepareExecute(transactionInfo);

		TransactionInput oInput = transactionInfo.getBody().getInput();
		AccountInfo.Builder sender = this.getAccount(oInput.getAddress().toByteArray());
		AccountInfo.Builder tokenRecordAccount = this.getAccount(this.iAccountHandler.tokenValueAddress());
		OwnerTokenData oOwnerTokenData = transactionInfo.getBody().getData().getOwnerTokenData();

		if (oOwnerTokenData.getToken() == null || oOwnerTokenData.getToken().equals(ByteString.EMPTY)) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, token name must not be empty"));
		} else if (oOwnerTokenData.getToken().size() > 32) {
			throw new TransactionParameterInvalidException(String.format("parameter invalid, token name invalid"));
		} else if (BytesHelper.bytesToBigInteger(oOwnerTokenData.getAmount().toByteArray())
				.compareTo(BigInteger.ZERO) < 0) {
			throw new TransactionParameterInvalidException(String.format("parameter invalid, token amount invalid"));
		}

		if (oOwnerTokenData.getOpCode() == OwnerTokenData.OwnerTokenOpCode.PUBLIC) {
			if (BytesHelper.bytesToBigInteger(oOwnerTokenData.getAmount().toByteArray())
					.compareTo(ActuatorConfig.minTokenTotal) == -1
					|| BytesHelper.bytesToBigInteger(oOwnerTokenData.getAmount().toByteArray())
							.compareTo(ActuatorConfig.maxTokenTotal) == 1) {
				throw new TransactionParameterInvalidException(
						String.format("parameter invalid, token amount must between %s and %s ",
								UnitHelper.fromWei(ActuatorConfig.minTokenTotal),
								UnitHelper.fromWei(ActuatorConfig.maxTokenTotal)));
			} else if (this.iAccountHandler.getBalance(sender)
					.compareTo(ActuatorConfig.token_create_lock_balance.add(fee)) == -1) {
				throw new TransactionParameterInvalidException(
						String.format("parameter invalid, not enough deposit to create token"));
			} else {
				TokenValue oTokenValue = this.iAccountHandler.getToken(tokenRecordAccount,
						oOwnerTokenData.getToken().toByteArray());
				if (oTokenValue != null) {
					throw new TransactionParameterInvalidException(
							String.format("parameter invalid, duplicate token name %s is not allowed",
									this.iCryptoHandler.bytesToHexStr(oOwnerTokenData.getToken().toByteArray())));
				}
			}
		} else if (oOwnerTokenData.getOpCode() == OwnerTokenData.OwnerTokenOpCode.BURN) {
			if (iAccountHandler.getBalance(sender).compareTo(ActuatorConfig.token_burn_lock_balance.add(fee)) == -1) {
				throw new TransactionParameterInvalidException(
						String.format("parameter invalid, not enough deposit to burn token"));
			}
			TokenValue oTokenValue = this.iAccountHandler.getToken(tokenRecordAccount,
					oOwnerTokenData.getToken().toByteArray());

			if (oTokenValue == null || !oTokenValue.getAddress().equals(oInput.getAddress())) {
				throw new TransactionParameterInvalidException(String.format("parameter invalid, token %s not exists",
						this.iCryptoHandler.bytesToHexStr(oOwnerTokenData.getToken().toByteArray())));
			}

			if (this.iAccountHandler.getTokenBalance(sender, oOwnerTokenData.getToken().toByteArray())
					.compareTo(BytesHelper.bytesToBigInteger(oOwnerTokenData.getAmount().toByteArray())) == -1) {
				throw new TransactionParameterInvalidException(
						String.format("parameter invalid, not enough token to burn"));
			}
		} else if (oOwnerTokenData.getOpCode() == OwnerTokenData.OwnerTokenOpCode.MINT) {
			if (iAccountHandler.getBalance(sender).compareTo(ActuatorConfig.token_mint_lock_balance.add(fee)) == -1) {
				throw new TransactionParameterInvalidException(
						String.format("parameter invalid, not enough deposit to mint token"));
			}

			TokenValue oTokenValue = this.iAccountHandler.getToken(tokenRecordAccount,
					oOwnerTokenData.getToken().toByteArray());

			if (oTokenValue == null || !oTokenValue.getAddress().equals(oInput.getAddress())) {
				throw new TransactionParameterInvalidException(String.format("parameter invalid, token %s not exists",
						this.iCryptoHandler.bytesToHexStr(oOwnerTokenData.getToken().toByteArray())));
			}
		}

	}
}