package org.fok.actuator.cryptotoken;

import com.google.protobuf.ByteString;

import org.fok.actuator.AbstractTransactionActuator;
import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountCryptoToken;
import org.fok.core.model.Account.AccountCryptoValue;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Account.AccountValue;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.core.model.Transaction.TransactionOutput;
import org.fok.tools.bytes.BytesComparisons;
import org.fok.tools.bytes.BytesHashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/8 DESC:
 */
public class ActuatorCryptoTokenTransaction extends AbstractTransactionActuator {
	public ActuatorCryptoTokenTransaction(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler,
			BlockInfo oBlock, ICryptoHandler iCryptoHandler) {
		super(iAccountHandler, iTransactionHandler, oBlock, iCryptoHandler);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 不校验发送方和接收方的balance的一致性
	 * 
	 * @param oMultiTransaction
	 * @param accounts
	 * @throws Exception
	 */
	@Override
	public void onPrepareExecute(TransactionInfo oMultiTransaction, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {
		List<String> inputTokens = new ArrayList<>();

		TransactionInput oInput = oMultiTransaction.getBody().getInputs();

		AccountInfo.Builder oAccount = accounts.get(oInput.getAddress().toByteArray());

		for (int i = 0; i < oInput.getCryptoTokenCount(); i++) {
			if (oInput.getCryptoToken(i) != null && !oInput.getCryptoToken(i).equals(ByteString.EMPTY)) {
				if (inputTokens.contains(this.iCryptoHandler.bytesToHexStr(oInput.getCryptoToken(i).toByteArray()))) {
					throw new TransactionParameterInvalidException("parameter invalid, duplicate token");
				} else
					inputTokens.add(this.iCryptoHandler.bytesToHexStr(oInput.getCryptoToken(i).toByteArray()));
			}

			AccountCryptoToken oAccountCryptoToken = this.iAccountHandler.getCryptoTokenBalance(oAccount,
					oInput.getSymbol().toByteArray(), oInput.getCryptoToken(i).toByteArray());
			if (oAccountCryptoToken == null) {
				throw new TransactionParameterInvalidException(String.format(
						"parameter invalid, input %s not found token [%s] with hash [%s]",
						this.iCryptoHandler.bytesToHexStr(oInput.getAddress().toByteArray()), oInput.getSymbol(),
						this.iCryptoHandler.bytesToHexStr(oInput.getCryptoToken(i).toByteArray())));
			}
		}
		for (int j = 0; j < oMultiTransaction.getBody().getOutputsCount(); j++) {
			TransactionOutput oOutput = oMultiTransaction.getBody().getOutputs(j);
			for (ByteString outputCryptoToken : oOutput.getCryptoTokenList()) {
				if (!inputTokens.remove(this.iCryptoHandler.bytesToHexStr(outputCryptoToken.toByteArray()))) {
					throw new TransactionParameterInvalidException(String.format(
							"parameter invalid, not found token %s with hash %s in input list", oInput.getSymbol(),
							this.iCryptoHandler.bytesToHexStr(outputCryptoToken.toByteArray())));
				}
			}
		}
		if (inputTokens.size() > 0) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, not found token %s with hash %s in output list",
							oInput.getSymbol(), inputTokens.get(0)));
		}

		super.onPrepareExecute(oMultiTransaction, accounts);
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {
		AccountInfo.Builder sender = accounts.get(transactionInfo.getBody().getInputs().getAddress().toByteArray());
		this.iAccountHandler.setNonce(sender, transactionInfo.getBody().getInputs().getNonce() + 1);
		BytesHashMap<AccountCryptoToken> tokens = new BytesHashMap<>();

		TransactionInput oInput = transactionInfo.getBody().getInputs();
		byte[] symbol = oInput.getSymbol().toByteArray();
		for (int i = 0; i < oInput.getCryptoTokenCount(); i++) {
			AccountCryptoToken oToken = this.iAccountHandler.getCryptoTokenBalance(sender,
					oInput.getSymbol().toByteArray(), oInput.getCryptoToken(i).toByteArray());
			tokens.put(oToken.getHash().toByteArray(), oToken);

			this.iAccountHandler.removeCryptoTokenBalance(sender, oInput.getSymbol().toByteArray(),
					oToken.getHash().toByteArray());
		}

		// 接收方增加balance
		for (int i = 0; i < transactionInfo.getBody().getOutputsCount(); i++) {
			TransactionOutput oOutput = transactionInfo.getBody().getOutputs(i);
			AccountInfo.Builder receiver = accounts.get(oOutput.getAddress().toByteArray());
			AccountCryptoToken.Builder oAccountCryptoToken = tokens.get(oOutput.getCryptoToken(i).toByteArray())
					.toBuilder();
			oAccountCryptoToken.setOwner(oOutput.getAddress());
			oAccountCryptoToken.setOwnertime(transactionInfo.getBody().getTimestamp());
			oAccountCryptoToken.setNonce(oAccountCryptoToken.getNonce() + 1);

			this.iAccountHandler.addCryptoTokenBalance(receiver, symbol, oAccountCryptoToken.build());
			tokens.remove(oOutput.getCryptoToken(i).toByteArray());

			AccountInfo.Builder cryptoAccount = accounts.get(this.iCryptoHandler.sha3(symbol));
			this.iAccountHandler.putAccountStorage(cryptoAccount, oOutput.getCryptoToken(i).toByteArray(),
					oAccountCryptoToken.build().toByteArray());
		}
		return ByteString.EMPTY;
	}
}
