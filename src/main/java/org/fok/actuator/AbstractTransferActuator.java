package org.fok.actuator;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionResultHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountCryptoToken;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Account.AccountValue;
import org.fok.core.model.Account.AccountInfo.Builder;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.core.model.Transaction.TransactionOutput;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;

import com.google.protobuf.ByteString;

public class AbstractTransferActuator extends AbstractTransactionActuator {

	public AbstractTransferActuator(IAccountHandler iAccountHandler, ITransactionResultHandler iTransactionHandler,
			ICryptoHandler iCryptoHandler, BlockInfo currentBlock) {
		super(iAccountHandler, iTransactionHandler, iCryptoHandler, currentBlock);
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo) throws Exception {
		TransactionInput oInput = transactionInfo.getBody().getInput();

		AccountInfo.Builder sender = getAccount(oInput.getAddress().toByteArray());

		iAccountHandler.setNonce(sender, oInput.getNonce() + 1);
		// 发送方移除主币余额
		iAccountHandler.subBalance(sender, totalAmount);
		// 发送方移除token余额
		for (byte[] token : totalTokens.keySet()) {
			BigInteger tokenAmount = totalTokens.get(token);
			iAccountHandler.subTokenBalance(sender, token, tokenAmount);
		}
		// 发送方移除cryptoToken余额
		for (byte[] cryptoToken : totalCryptoTokens.keySet()) {
			List<AccountCryptoToken.Builder> cryptoTokens = totalCryptoTokens.get(cryptoToken);
			for (AccountCryptoToken.Builder cToken : cryptoTokens) {
				this.iAccountHandler.removeCryptoTokenBalance(sender, cryptoToken, cToken.getHash().toByteArray());
			}
		}

		this.putAccount(sender);

		for (TransactionOutput oOutput : transactionInfo.getBody().getOutputsList()) {
			AccountInfo.Builder receiver = getAccount(oOutput.getAddress().toByteArray());
			// 接收方增加主币余额
			this.iAccountHandler.addBalance(receiver, BytesHelper.bytesToBigInteger(oOutput.getAmount().toByteArray()));
			// 接收方增加token余额
			if (oOutput.getToken() != null && !oOutput.getToken().equals(ByteString.EMPTY)) {
				this.iAccountHandler.addTokenBalance(receiver, oOutput.getToken().toByteArray(),
						BytesHelper.bytesToBigInteger(oOutput.getTokenAmount().toByteArray()));
			}
			// 接收方增加cryptotoken余额
			if (oOutput.getSymbol() != null && !oOutput.getSymbol().equals(ByteString.EMPTY)
					&& oOutput.getCryptoTokenCount() > 0) {

				AccountInfo.Builder cryptoAccount = getAccount(
						this.iCryptoHandler.sha3(oOutput.getSymbol().toByteArray()));
				for (ByteString cryptoTokenHash : oOutput.getCryptoTokenList()) {
					for (AccountCryptoToken.Builder cryptoToken : totalCryptoTokens
							.get(oOutput.getSymbol().toByteArray())) {
						if (cryptoToken.getHash().equals(cryptoTokenHash)) {
							cryptoToken.setOwner(oOutput.getAddress());
							cryptoToken.setOwnertime(transactionInfo.getBody().getTimestamp());
							cryptoToken.setNonce(cryptoToken.getNonce() + 1);

							this.iAccountHandler.addCryptoTokenBalance(receiver, oOutput.getSymbol().toByteArray(),
									cryptoToken.build());
							this.iAccountHandler.putAccountStorage(cryptoAccount, cryptoToken.getHash().toByteArray(),
									cryptoToken.build().toByteArray());
						}
					}
				}

				this.putAccount(cryptoAccount);
			}

			this.putAccount(receiver);
		}
		return ByteString.EMPTY;
	}

	@Override
	public void onPrepareExecute(TransactionInfo transactionInfo) throws Exception {
		this.reset();
		super.onPrepareExecute(transactionInfo);

		if (transactionInfo.getBody().getData() != null) {
			throw new TransactionParameterInvalidException("parameter invalid, data in tx body must null");
		}

		TransactionInput oInput = transactionInfo.getBody().getInput();

		AccountInfo.Builder sender = getAccount(oInput.getAddress().toByteArray());
		AccountValue.Builder senderAccountValue = sender.getValue().toBuilder();

		// 联合账户不允许使用该交易
		if (senderAccountValue.getAddressCount() > 0) {
			throw new TransactionParameterInvalidException(
					"parameter invalid, union account does not allow to create this transaction");
		}

		if (transactionInfo.getBody().getOutputsCount() == 0) {
			throw new TransactionParameterInvalidException("parameter invalid, outputs must not be null");
		}

		if (transactionInfo.getBody().getFee() != null
				&& !transactionInfo.getBody().getFee().equals(ByteString.EMPTY)) {
			fee = BytesHelper.bytesToBigInteger(transactionInfo.getBody().getFee().toByteArray());
		}

		if (fee.compareTo(BigInteger.ZERO) < 0) {
			throw new TransactionParameterInvalidException("parameter invalid, transaction value must large than 0");
		}
		totalAmount = totalAmount.add(fee);
		/*
		 * 资产交易的组合方式 主币、token、cryptoToken、主币 + token + cryptoToken
		 * TransactionOutput只允许包含一种类型的token和cryptoToken，
		 * 如果要在交易种发送多种类型的token给同一个账户， 可以增加另外一个TransactionOutput
		 */
		for (TransactionOutput oOutput : transactionInfo.getBody().getOutputsList()) {
			// 主币
			BigInteger outputAmount = BytesHelper.bytesToBigInteger(oOutput.getAmount().toByteArray());
			if (outputAmount.compareTo(BigInteger.ZERO) < 0) {
				throw new TransactionParameterInvalidException("parameter invalid, amount must large than 0");
			}
			totalAmount = totalAmount.add(outputAmount);

			// token
			if (oOutput.getToken() != null && !oOutput.getToken().equals(ByteString.EMPTY)) {
				BigInteger tokenTotalAmount = totalTokens.get(oOutput.getToken().toByteArray());
				BigInteger tokenAmount = BytesHelper.bytesToBigInteger(oOutput.getTokenAmount().toByteArray());
				if (tokenAmount.compareTo(BigInteger.ZERO) < 0) {
					throw new TransactionParameterInvalidException("parameter invalid, token amount must large than 0");
				}

				if (tokenTotalAmount == null) {
					tokenTotalAmount = tokenAmount;
				} else {
					tokenTotalAmount = tokenTotalAmount.add(tokenAmount);
				}
				totalTokens.put(oOutput.getToken().toByteArray(), tokenTotalAmount);
			}

			// cryptoToken
			if (oOutput.getSymbol() != null && !oOutput.getSymbol().equals(ByteString.EMPTY)
					&& oOutput.getCryptoTokenCount() > 0) {
				List<AccountCryptoToken.Builder> cTokens = totalCryptoTokens.get(oOutput.getSymbol().toByteArray());
				if (cTokens == null) {
					cTokens = new ArrayList<>();
				}
				for (ByteString transferCryptoToken : oOutput.getCryptoTokenList()) {
					// 判断发送方账户的cryptotoken余额
					AccountCryptoToken oAccountCryptoToken = this.iAccountHandler.getCryptoTokenBalance(sender,
							oOutput.getSymbol().toByteArray(), transferCryptoToken.toByteArray());

					if (oAccountCryptoToken == null) {
						throw new TransactionParameterInvalidException(
								"parameter invalid, sender not have crypto token");
					} else if (cTokens.contains(transferCryptoToken)) {
						throw new TransactionParameterInvalidException(
								"parameter invalid, duplicate crypto-token is not allowed");
					}
					cTokens.add(oAccountCryptoToken.toBuilder());
				}
				totalCryptoTokens.put(oOutput.getSymbol().toByteArray(), cTokens);
			}
		}

		// 判断发送方账户的主币余额
		// 总额 = value + totalAmount
		if (this.iAccountHandler.getBalance(sender).compareTo(totalAmount) < 0) {
			throw new TransactionParameterInvalidException("parameter invalid, balance of the sender is not enough");
		}

		// 判断发送方账户的token余额
		for (byte[] token : totalTokens.keySet()) {
			BigInteger tokenBalance = this.iAccountHandler.getTokenBalance(sender, token);
			BigInteger tokenTotalAmount = totalTokens.get(token);
			if (tokenBalance.compareTo(tokenTotalAmount) < 0) {
				throw new TransactionParameterInvalidException(
						"parameter invalid, sender not have enough token balance");
			}
		}
	}
}
