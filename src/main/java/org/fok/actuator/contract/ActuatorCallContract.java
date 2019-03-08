package org.fok.actuator.contract;

import com.google.protobuf.ByteString;

import lombok.extern.slf4j.Slf4j;

import org.fok.actuator.AbstractTransactionActuator;
import org.fok.actuator.evmapi.AccountEVMHandler;
import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.core.model.Transaction.TransactionOutput;
import org.fok.tools.bytes.BytesHashMap;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/7 DESC:
 */
@Slf4j
public class ActuatorCallContract extends AbstractTransactionActuator {
	public ActuatorCallContract(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler,
			BlockInfo oBlock, ICryptoHandler iEncApiHandler) {
		super(iAccountHandler, iTransactionHandler, oBlock, iEncApiHandler);
	}

	@Override
	public void onPrepareExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {
		if (transactionInfo.getBody().getInputs() == null || transactionInfo.getBody().getOutputsCount() != 1) {
			throw new TransactionParameterInvalidException(
					"parameter invalid, the inputs and outputs must be only one");
		}
		TransactionOutput output = transactionInfo.getBody().getOutputs(0);

		AccountInfo.Builder account = accounts.get(output.getAddress().toByteArray());
		if (account.getValue().getCode() == null) {
			throw new TransactionParameterInvalidException(
					"parameter invalid, address " + this.iCryptoHandler.bytesToHexStr(output.getAddress().toByteArray())
							+ " is not validate contract.");
		}

		super.onPrepareExecute(transactionInfo, accounts);
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {
		// VM vm = new VM();
		AccountInfo.Builder existsContract = accounts
				.get(transactionInfo.getBody().getOutputs(0).getAddress().toByteArray());
		AccountInfo.Builder callAccount = accounts
				.get(transactionInfo.getBody().getInputs().getAddress().toByteArray());

		this.iAccountHandler.setNonce(callAccount, transactionInfo.getBody().getInputs().getNonce() + 1);
		this.iAccountHandler.increaseNonce(existsContract);

		AccountEVMHandler accountEVMHandler = new AccountEVMHandler();
		accountEVMHandler.setAccountHandler(this.iAccountHandler);
		accountEVMHandler.setTransactionHandler(this.iTransactionHandler);
		accountEVMHandler.setCryptoHandler(this.iCryptoHandler);
		accountEVMHandler.setTouchAccount(accounts);

		TransactionInput oInput = transactionInfo.getBody().getInputs();
		// TODO evm
		// ProgramInvokeImpl programInvoke = new
		// ProgramInvokeImpl(existsContract.getAddress().toByteArray(),
		// callAccount.getAddress().toByteArray(),
		// callAccount.getAddress().toByteArray(),
		// oInput.getAmount().toByteArray(),
		// BytesHelper.bigIntegerToBytes(BigInteger.ZERO),
		// transactionInfo.getTxBody().getData().toByteArray(),
		// this.blockInfo.getHeader().getParentHash().toByteArray(), null,
		// this.blockInfo.getHeader().getTimestamp(),
		// this.blockInfo.getHeader().getHeight(),
		// ByteString.EMPTY.toByteArray(), accountEVMHandler);
		//
		// Program program = new
		// Program(existsContract.getValue().getCodeHash().toByteArray(),
		// existsContract.getValue().getCode().toByteArray(), programInvoke,
		// transactionInfo);
		// vm.play(program);
		// ProgramResult result = program.getResult();
		//
		// if (result.getException() != null || result.isRevert()) {
		// if (result.getException() != null) {
		// log.error("error on call conntract", result.getException());
		// throw new TransactionExecuteException("error on call contract");
		// } else {
		// throw new TransactionExecuteException("REVERT opcode executed");
		// }
		// } else {
		// return ByteString.copyFrom(result.getHReturn());
		// }

		return ByteString.EMPTY;
	}
}
