pragma solidity ^0.4.22;

contract Seller{

  mapping(string=>string) sellerDataBase;
  mapping(string=>address) sellerAddress;

  address owner;

  constructor() public {
    owner = msg.sender;
  }

  function isMaster(address checkAddress) private view returns(bool){
    bool sign = false;
    if(owner == checkAddress){
      sign = true;
    }
    return sign;
  }

  function setSellerData(string clothIDAndSellerID,string clothData) public {
    if(isMaster(msg.sender)){
      sellerDataBase[clothIDAndSellerID] = clothData;
    }
  }
  function getSellerData(string clothIDAndSellerID)public view returns(string){
    return sellerDataBase[clothIDAndSellerID];
  }

  function setSellerAddress(string sellerID,address sellerAddr) public {
    if(isMaster(msg.sender)){
      sellerAddress[sellerID] = sellerAddr;
    }
  }
  function getSellerAddress(string sellerID) public view returns(address){
      return sellerAddress[sellerID];
  }


}
