package org.fok.actuator.unionaccount;

import com.google.protobuf.ByteString;
import org.fok.actuator.AbstractTransactionActuator;
import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.actuator.exception.TransactionVerifyException;
import org.fok.actuator.util.DateTimeUtil;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Account.AccountValue;
import org.fok.core.model.Account.UnionAccountStorage;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionBody;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.core.model.Transaction.TransactionSignature;
import org.fok.tools.bytes.BytesComparisons;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;

import java.math.BigInteger;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/14 DESC:
 */
public class ActuatorUnionAccountTransaction extends AbstractTransactionActuator {
	public ActuatorUnionAccountTransaction(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler,
			BlockInfo blockInfo, ICryptoHandler iCryptoHandler) {
		super(iAccountHandler, iTransactionHandler, blockInfo, iCryptoHandler);
	}

	@Override
	public void onPrepareExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {

		if (transactionInfo.getBody().getInputs() == null || transactionInfo.getBody().getOutputsCount() != 1) {
			throw new TransactionParameterInvalidException("parameter invalid, inputs or outputs must be only one");
		}

		TransactionInput oInput = transactionInfo.getBody().getInputs();

		AccountInfo.Builder unionAccount = accounts.get(oInput.getAddress().toByteArray());
		AccountValue.Builder unionAccountValue = unionAccount.getValue().toBuilder();
		int txNonce = oInput.getNonce();
		int nonce = unionAccountValue.getNonce();
		if (nonce > txNonce) {
			throw new TransactionParameterInvalidException(
					String.format("sender nonce %s is not equal with transaction nonce %s", nonce, nonce));
		}

		BigInteger amount = BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray());
		BigInteger unionAccountBalance = BytesHelper.bytesToBigInteger(unionAccountValue.getBalance().toByteArray());
		BigInteger acceptMax = BytesHelper.bytesToBigInteger(unionAccountValue.getAcceptMax().toByteArray());
		BigInteger max = BytesHelper.bytesToBigInteger(unionAccountValue.getMax().toByteArray());

		if (amount.compareTo(BigInteger.ZERO) <= 0) {
			throw new TransactionParameterInvalidException("parameter invalid, amount invalidate");
		}

		if (amount.compareTo(unionAccountBalance) > 0) {
			throw new TransactionParameterInvalidException(
					String.format("sender balance %s less than %s", unionAccountBalance, amount));
		}

		if (amount.compareTo(max) > 0 && max.compareTo(BigInteger.ZERO) > 0) {
			throw new TransactionParameterInvalidException("parameter invalid, amount must small than " + max);
		}

		if (!BytesComparisons.equal(oInput.getAmount().toByteArray(),
				transactionInfo.getBody().getOutputs(0).getAmount().toByteArray())) {
			throw new TransactionParameterInvalidException("parameter invalid, transaction value not equal");
		}

		if ((amount.compareTo(acceptMax) >= 0 && acceptMax.compareTo(BigInteger.ZERO) > 0)
				|| acceptMax.compareTo(BigInteger.ZERO) == 0) {
			if (transactionInfo.getBody().getData() != null && !transactionInfo.getBody().getData().isEmpty()) {

				byte[] confirmTxBytes = this.iAccountHandler.getAccountStorage(unionAccount,
						transactionInfo.getBody().getData().toByteArray());
				UnionAccountStorage oUnionAccountStorage = UnionAccountStorage.parseFrom(confirmTxBytes);

				if (!BytesComparisons.equal(oUnionAccountStorage.getToAddress().toByteArray(),
						transactionInfo.getBody().getOutputs(0).getAddress().toByteArray())) {
					throw new TransactionParameterInvalidException(
							"parameter invalid, output address are equal with original tx");
				}

				boolean isAlreadyConfirm = false;
				boolean isExistsConfirmTx = false;
				for (int i = 0; i < oUnionAccountStorage.getAddressCount(); i++) {
					if (BytesComparisons.equal(oUnionAccountStorage.getAddress(i).toByteArray(),
							transactionInfo.getBody().getExdata().toByteArray())) {
						isAlreadyConfirm = true;
						break;
					}
					if (oUnionAccountStorage.getTxHash(i).equals(transactionInfo.getBody().getData())) {
						isExistsConfirmTx = true;
					}
				}
				if (isAlreadyConfirm) {
					throw new TransactionParameterInvalidException(
							"parameter invalid, transaction already confirmed by address " + this.iCryptoHandler
									.bytesToHexStr(transactionInfo.getBody().getExdata().toByteArray()));
				}
				if (!isExistsConfirmTx) {
					throw new TransactionParameterInvalidException(
							"parameter invalid, not found transaction need to be confirmed");
				}
				if (!BytesComparisons.equal(oUnionAccountStorage.getAmount().toByteArray(),
						oInput.getAmount().toByteArray())) {
					throw new TransactionParameterInvalidException(
							"parameter invalid, transaction amount not equal with original transaction");
				}

				if (oUnionAccountStorage != null) {
					if (oUnionAccountStorage.getAddressCount() >= unionAccountValue.getAcceptLimit()) {
						throw new TransactionParameterInvalidException(
								"parameter invalid, transaction already confirmed");
					}
				}

				if (oUnionAccountStorage.getAddressCount() + 1 == unionAccountValue.getAcceptLimit()) {
					if (DateTimeUtil.isToday(transactionInfo.getBody().getTimestamp(),
							unionAccountValue.getAccumulatedTimestamp())) {
						BigInteger totalMax = BytesHelper
								.bytesToBigInteger(unionAccountValue.getAccumulated().toByteArray());
						if (amount.add(totalMax).compareTo(max) > 0 && max.compareTo(BigInteger.ZERO) > 0) {
							throw new TransactionParameterInvalidException(
									"parameter invalid, already more than the maximum transfer amount of the day");
						}
					}
				}
			} else {
				BigInteger totalMax = BytesHelper.bytesToBigInteger(unionAccountValue.getAccumulated().toByteArray());
				if (amount.add(totalMax).compareTo(max) > 0 && max.compareTo(BigInteger.ZERO) > 0) {
					throw new TransactionParameterInvalidException(
							"parameter invalid, already more than the maximum transfer amount of the day");
				}
			}
		} else {
			BigInteger totalMax = BytesHelper.bytesToBigInteger(unionAccountValue.getAccumulated().toByteArray());
			if (amount.add(totalMax).compareTo(max) > 0 && max.compareTo(BigInteger.ZERO) > 0) {
				throw new TransactionParameterInvalidException(
						"parameter invalid, already more than the maximum transfer amount of the day");
			}
		}
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {

		TransactionInput oInput = transactionInfo.getBody().getInputs();
		AccountInfo.Builder unionAccount = accounts.get(oInput.getAddress().toByteArray());
		AccountValue.Builder unionAccountValue = unionAccount.getValue().toBuilder();

		BigInteger amount = BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray());
		BigInteger acceptMax = BytesHelper.bytesToBigInteger(unionAccountValue.getAcceptMax().toByteArray());

		if (amount.compareTo(acceptMax) >= 0) {
			if (transactionInfo.getBody().getData() == null || transactionInfo.getBody().getData().isEmpty()) {
				// first commit
				unionAccountValue.setNonce(oInput.getNonce() + 1);
				unionAccount.setValue(unionAccountValue);

				UnionAccountStorage.Builder oUnionAccountStorage = UnionAccountStorage.newBuilder();
				oUnionAccountStorage.addAddress(transactionInfo.getBody().getExdata());
				oUnionAccountStorage.addTxHash(transactionInfo.getHash());
				oUnionAccountStorage.setAmount(oInput.getAmount());
				oUnionAccountStorage.setToAddress(transactionInfo.getBody().getOutputs(0).getAddress());

				this.iAccountHandler.putAccountStorage(unionAccount, transactionInfo.getHash().toByteArray(),
						oUnionAccountStorage.build().toByteArray());
				return ByteString.EMPTY;
			} else {
				byte[] confirmTxBytes = this.iAccountHandler.getAccountStorage(unionAccount,
						transactionInfo.getBody().getData().toByteArray());
				UnionAccountStorage.Builder oUnionAccountStorage = UnionAccountStorage.parseFrom(confirmTxBytes)
						.toBuilder();
				oUnionAccountStorage.addAddress(transactionInfo.getBody().getExdata());
				oUnionAccountStorage.addTxHash(transactionInfo.getHash());

				this.iAccountHandler.putAccountStorage(unionAccount,
						transactionInfo.getBody().getData().toByteArray(),
						oUnionAccountStorage.build().toByteArray());

				unionAccountValue = unionAccount.getValue().toBuilder();
				if (oUnionAccountStorage.getAddressCount() != unionAccountValue.getAcceptLimit()) {
					// need more confirm
					unionAccountValue.setNonce(oInput.getNonce() + 1);
					unionAccount.setValue(unionAccountValue);

					return ByteString.EMPTY;
				}
			}
		}
		unionAccountValue = unionAccount.getValue().toBuilder();
		if (DateTimeUtil.isToday(transactionInfo.getBody().getTimestamp(),
				unionAccountValue.getAccumulatedTimestamp())) {
			BigInteger accumulated = BytesHelper.bytesToBigInteger(unionAccountValue.getAccumulated().toByteArray());
			unionAccountValue.setAccumulated(ByteString.copyFrom(BytesHelper.bigIntegerToBytes(accumulated.add(amount))));
		} else {
			unionAccountValue.setAccumulated(ByteString.copyFrom(BytesHelper.bigIntegerToBytes(amount)));
		}
		unionAccountValue.setAccumulatedTimestamp(transactionInfo.getBody().getTimestamp());
		unionAccountValue.setNonce(oInput.getNonce() + 1);
		unionAccount.setValue(unionAccountValue);

		return super.onExecute(transactionInfo, accounts);
	}

	@Override
	public void onVerifySignature(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {
		TransactionInput oInput = transactionInfo.getBody().getInputs();
		AccountInfo.Builder sender = accounts.get(oInput.getAddress().toByteArray());
		AccountValue.Builder senderAccountValue = sender.getValue().toBuilder();

		TransactionInfo.Builder signatureTx = transactionInfo.toBuilder();
		TransactionBody.Builder txBody = signatureTx.getBodyBuilder();
		signatureTx.clearHash();
		txBody = txBody.clearSignatures();
		byte[] oMultiTransactionEncode = txBody.build().toByteArray();
		TransactionSignature oMultiTransactionSignature = transactionInfo.getBody().getSignatures();
		byte[] hexPubKey = this.iCryptoHandler.signatureToKey(oMultiTransactionEncode,
				oMultiTransactionSignature.getSignature().toByteArray());
		byte[] hexAddress = this.iCryptoHandler.signatureToAddress(oMultiTransactionEncode,
				oMultiTransactionSignature.getSignature().toByteArray());

		boolean isRelAddress = false;
		for (ByteString relAddress : senderAccountValue.getAddressList()) {
			if (BytesComparisons.equal(hexAddress, relAddress.toByteArray())) {
				isRelAddress = true;
				break;
			}
		}
		if (isRelAddress) {
			if (!this.iCryptoHandler.verify(hexPubKey, oMultiTransactionEncode,
					oMultiTransactionSignature.getSignature().toByteArray())) {
				throw new TransactionVerifyException(String.format("signature %s verify fail with pubkey %s",
						this.iCryptoHandler.bytesToHexStr(oMultiTransactionSignature.getSignature().toByteArray()),
						hexPubKey));
			}
		} else {
			throw new TransactionVerifyException(
					"signature verify fail, current account are not allowed to initiate transactions");
		}

		if (!BytesComparisons.equal(transactionInfo.getBody().getExdata().toByteArray(), hexAddress)) {
			throw new TransactionVerifyException("signature verify fail, transaction data not equal with Signer");
		}
	}
}
