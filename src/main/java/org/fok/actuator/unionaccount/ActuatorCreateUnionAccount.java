package org.fok.actuator.unionaccount;

import com.google.protobuf.ByteString;

import org.fok.actuator.AbstractTransactionActuator;
import org.fok.actuator.AbstractUnTransferActuator;
import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionResultHandler;
import org.fok.core.crypto.model.KeyPairs;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Account.AccountValue;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;

import java.math.BigInteger;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/8 DESC:
 */
//public class ActuatorCreateUnionAccount extends AbstractUnTransferActuator {
//	public ActuatorCreateUnionAccount(IAccountHandler iAccountHandler, ITransactionResultHandler iTransactionHandler,
//			ICryptoHandler iCryptoHandler, BlockInfo blockInfo, BytesHashMap<AccountInfo.Builder> accounts) {
//		super(iAccountHandler, iTransactionHandler, iCryptoHandler, blockInfo, accounts);
//	}
//
//	@Override
//	public void onPrepareExecute(TransactionInfo transactionInfo) throws Exception {
//
//		if (transactionInfo.getBody().getInputs() == null) {
//			throw new TransactionParameterInvalidException("parameter invalid, inputs must be only one");
//		}
//
//		TransactionInput oInput = transactionInfo.getBody().getInputs();
//
//		if (BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray()).compareTo(BigInteger.ZERO) != 0) {
//			throw new TransactionParameterInvalidException("parameter invalid, amount must be zero");
//		}
//
//		if (transactionInfo.getBody().getOutputsCount() != 0) {
//			throw new TransactionParameterInvalidException("parameter invalid, inputs must be empty");
//		}
//
//		AccountInfo.Builder sender = accounts.get(oInput.getAddress().toByteArray());
//		AccountValue.Builder senderAccountValue = sender.getValue().toBuilder();
//		int txNonce = oInput.getNonce();
//		int nonce = senderAccountValue.getNonce();
//		if (nonce > txNonce) {
//			throw new TransactionParameterInvalidException(String
//					.format("parameter invalid, sender nonce %s is not equal with transaction nonce %s", nonce, nonce));
//		}
//
//		UnionAccountData oUnionAccountData = UnionAccountData
//				.parseFrom(transactionInfo.getBody().getData().toByteArray());
//		if (oUnionAccountData == null || oUnionAccountData.getMax() == null || oUnionAccountData.getAcceptLimit() < 0
//				|| oUnionAccountData.getAcceptMax() == null || oUnionAccountData.getAddressCount() < 2) {
//			throw new TransactionParameterInvalidException("parameter invalid, union account info invalidate");
//		}
//
//		if (oUnionAccountData.getAcceptLimit() > oUnionAccountData.getAddressCount()) {
//			throw new TransactionParameterInvalidException(
//					"parameter invalid, AcceptLimit count must smaller than address count");
//		}
//	}
//
//	@Override
//	public ByteString onExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
//			throws Exception {
//		TransactionInput oInput = transactionInfo.getBody().getInputs();
//		AccountInfo.Builder sender = accounts.get(oInput.getAddress().toByteArray());
//		iAccountHandler.setNonce(sender, oInput.getNonce() + 1);
//
//		UnionAccountData oUnionAccountData = UnionAccountData
//				.parseFrom(transactionInfo.getBody().getData().toByteArray());
//
//		KeyPairs keyPair = this.iCryptoHandler.genAccountKey();
//		AccountInfo.Builder oUnionAccount = this.iAccountHandler.createUnionAccount(
//				ByteString.copyFrom(iCryptoHandler.hexStrToBytes(keyPair.getAddress())), oUnionAccountData.getMax(),
//				oUnionAccountData.getAcceptMax(), oUnionAccountData.getAcceptLimit(),
//				oUnionAccountData.getAddressList());
//
//		accounts.put(iCryptoHandler.hexStrToBytes(keyPair.getAddress()), oUnionAccount);
//		return ByteString.copyFrom(iCryptoHandler.hexStrToBytes(keyPair.getAddress()));
//	}
//}
