package org.fok.actuator.contract;

import com.google.protobuf.ByteString;

import lombok.extern.slf4j.Slf4j;

import org.fok.actuator.AbstractUnTransferActuator;
import org.fok.actuator.config.ActuatorConfig;
import org.fok.actuator.evmapi.AccountEVMHandler;
import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionResultHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.core.model.Transaction.TransactionData.CallContractData;
import org.fok.tools.bytes.BytesHashMap;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/7 DESC:
 */
@Slf4j
public class ActuatorCallContract extends AbstractUnTransferActuator {
	public ActuatorCallContract(IAccountHandler iAccountHandler, ITransactionResultHandler iTransactionHandler,
			ICryptoHandler iEncApiHandler, BlockInfo oBlock) {
		super(iAccountHandler, iTransactionHandler, iEncApiHandler, oBlock);
	}

	@Override
	public void onPrepareExecute(TransactionInfo transactionInfo) throws Exception {
		super.onPrepareExecute(transactionInfo);

		CallContractData oCallContractData = transactionInfo.getBody().getData().getCallContractData();

		AccountInfo.Builder account = this.getAccount(oCallContractData.getContract());
		if (account.getValue().getCode() == null) {
			throw new TransactionParameterInvalidException("parameter invalid, address "
					+ this.iCryptoHandler.bytesToHexStr(oCallContractData.getContract().toByteArray())
					+ " is not validate contract.");
		}

		AccountInfo.Builder callAccount = this
				.getAccount(transactionInfo.getBody().getInput().getAddress());
		if (this.iAccountHandler.getBalance(callAccount).compareTo(fee) < 0) {
			throw new TransactionParameterInvalidException("parameter invalid, not have enough balance");
		}
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo) throws Exception {
		// VM vm = new VM();
		TransactionInput oInput = transactionInfo.getBody().getInput();
		
		AccountInfo.Builder lockAccount = this
				.getAccount(this.iCryptoHandler.hexStrToBytes(ActuatorConfig.lock_account_address));
		CallContractData oCallContractData = transactionInfo.getBody().getData().getCallContractData();
		AccountInfo.Builder existsContract = this.getAccount(oCallContractData.getContract());
		AccountInfo.Builder callAccount = this
				.getAccount(transactionInfo.getBody().getInput().getAddress());

		this.iAccountHandler.setNonce(callAccount, oInput.getNonce() + 1);
		this.iAccountHandler.subBalance(callAccount, fee);
		this.iAccountHandler.increaseNonce(existsContract);
		this.iAccountHandler.addBalance(lockAccount, fee);
		
		AccountEVMHandler accountEVMHandler = new AccountEVMHandler();
		accountEVMHandler.setAccountHandler(this.iAccountHandler);
		accountEVMHandler.setCryptoHandler(this.iCryptoHandler);
		// accountEVMHandler.setTouchAccount(accounts);

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
