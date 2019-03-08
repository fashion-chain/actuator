package org.fok.actuator.contract;

import com.google.protobuf.ByteString;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import org.fok.actuator.AbstractTransactionActuator;
import org.fok.actuator.config.ActuatorConfig;
import org.fok.actuator.evmapi.AccountEVMHandler;
import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;
import org.fok.tools.rlp.FokRLP;
import java.math.BigInteger;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/7 DESC:
 */
public class ActuatorCreateContract extends AbstractTransactionActuator {
	@ActorRequire(name = "BlockChain_Config", scope = "global")
	ActuatorConfig actuatorConfig;

	public ActuatorCreateContract(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler,
			BlockInfo blockInfo, ICryptoHandler iCryptoHandler) {
		super(iAccountHandler, iTransactionHandler, blockInfo, iCryptoHandler);
	}

	@Override
	public void onPrepareExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {
		if (transactionInfo.getBody().getInputs() == null) {
			throw new TransactionParameterInvalidException("parameter invalid, inputs must be only one");
		}
		if (transactionInfo.getBody().getOutputsCount() != 0) {
			throw new TransactionParameterInvalidException("parameter invalid, outputs must be null");
		}

		AccountInfo.Builder sender = accounts.get(transactionInfo.getBody().getInputs().getAddress().toByteArray());
		BigInteger totalBalance = iAccountHandler.getBalance(sender);

		if (totalBalance
				.compareTo(ActuatorConfig.contract_create_lock_balance) == -1) {
			throw new Exception(String.format("not enough deposit to create contract"));
		}

		super.onPrepareExecute(transactionInfo, accounts);
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {
		byte[] newContractAddress = this.iCryptoHandler
				.sha256(FokRLP.encodeList(transactionInfo.getBody().getInputs().getAddress().toByteArray(),
						BytesHelper.intToBytes(transactionInfo.getBody().getInputs().getNonce())));

		AccountInfo.Builder contract = this.iAccountHandler.createAccount(newContractAddress);
		accounts.put(newContractAddress, contract);

		TransactionInput oInput = transactionInfo.getBody().getInputs();
		AccountInfo.Builder sender = accounts.get(oInput.getAddress().toByteArray());
		
		this.iAccountHandler.subBalance(sender, ActuatorConfig.contract_create_lock_balance);
		this.iAccountHandler.setNonce(sender, oInput.getNonce() + 1);

		AccountEVMHandler accountEVMHandler = new AccountEVMHandler();
		accountEVMHandler.setAccountHandler(this.iAccountHandler);
		accountEVMHandler.setTransactionHandler(this.iTransactionHandler);
		accountEVMHandler.setCryptoHandler(this.iCryptoHandler);
		accountEVMHandler.setTouchAccount(accounts);

		// 要返回合约地址
		// TODO evm
//		ProgramInvokeImpl createProgramInvoke = new ProgramInvokeImpl(newContractAddress,
//				oInput.getAddress().toByteArray(), oInput.getAddress().toByteArray(), oInput.getAmount().toByteArray(),
//				BytesHelper.bigIntegerToBytes(BigInteger.ZERO), transactionInfo.getTxBody().getData().toByteArray(),
//				this.blockInfo.getHeader().getParentHash().toByteArray(),
//				this.blockInfo.getMiner().getAddress().toByteArray(), this.blockInfo.getHeader().getTimestamp(),
//				this.blockInfo.getHeader().getHeight(), ByteString.EMPTY.toByteArray(), accountEVMHandler);
//
//		Program createProgram = new Program(transactionInfo.getTxBody().getData().toByteArray(), createProgramInvoke,
//				transactionInfo);
//		VM createVM = new VM();
//		createVM.play(createProgram);
//		ProgramResult createResult = createProgram.getResult();
//		if (createResult.getException() != null) {
//			return ByteString.copyFromUtf8(createResult.getException().getMessage());
//		} else {
//			createResult = createProgram.getResult();
//
//			AccountValue.Builder oContractValue = contract.getValueBuilder();
//			oContractValue.setCode(ByteString.copyFrom(createResult.getHReturn()));
//			oContractValue.setCodeHash(
//					ByteString.copyFrom(this.iCryptoHandler.sha256(oContractValue.getCode().toByteArray())));
//			oContractValue.setData(transactionInfo.getTxBody().getExdata());
//			oContractValue.addAddress(transactionInfo.getTxBody().getInputs(0).getAddress());
//		
//			AccountInfo.Builder locker = accounts.get(iAccountHandler.lockBalanceAddress());
//			iAccountHandler.addBalance(locker, this.actuatorConfig.getContract_lock_balance());
//			return ByteString.EMPTY;
//		}
		return ByteString.EMPTY;
	}

}
