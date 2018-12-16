pragma solidity ^0.4.22;

contract PlatformSide{

  address platformAddr;


  mapping(string=>uint256) orderGas;



  address owner;
  constructor() public{
    owner = msg.sender;
  }

  //Check if it is the creator
  function isMaster(address checkAddress) private view returns(bool){
    bool sign = false;
    if(owner == checkAddress){
      sign = true;
    }
    return sign;
  }

  //Set up a transfer address
  function setTransferAddress(address _transferAddr) public {
    if(isMaster(msg.sender)){
      platformAddr = _transferAddr;
    }
  }
  //Get transfer address
  function getTransferAddress() public view returns(address){
    return platformAddr;
  }


  //Set up a gas
  function setGas(string oriderID,uint256 _gasvalue) public {
    if(isMaster(msg.sender)){
      orderGas[oriderID] = _gasvalue;
    }
  }
  //Get gas
  function getGas(string oriderID) public view returns(uint256){
    return orderGas[oriderID];
  }

}
