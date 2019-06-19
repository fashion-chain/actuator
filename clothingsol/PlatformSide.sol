pragma solidity ^0.4.22;

library SafeMath {
      function mul(uint256 a, uint256 b) internal pure returns (uint256) {
        if (a == 0) {
          return 0;
        }
        uint256 c = a * b;
        assert(c / a == b);
        return c;
      }

      function div(uint256 a, uint256 b) internal pure returns (uint256) {
        // assert(b > 0); // Solidity automatically throws when dividing by 0
        uint256 c = a / b;
        // assert(a == b * c + a % b); // There is no case in which this doesn't hold
        return c;
      }

      function sub(uint256 a, uint256 b) internal pure returns (uint256) {
        assert(b <= a);
        return a - b;
      }

      function add(uint256 a, uint256 b) internal pure returns (uint256) {
        uint256 c = a + b;
        assert(c >= a);
        return c;
      }
    }

contract PlatformSide{

  using SafeMath for uint256;
  address platformAddr;


  mapping(string=>uint256) orderGas;



  address owner;
  constructor() public{
    owner = msg.sender;
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

  //Check if it is the creator
  function isMaster(address checkAddress) private view returns(bool){
    bool sign = false;
    if(owner == checkAddress){
      sign = true;
    }
    return sign;
  }

  //Get gas
  function getGas(string oriderID) public view returns(uint256){
    return orderGas[oriderID];
  }

  function computerIssue(uint256 clothPrice,uint256 clothsols)public view returns(uint256){
    return clothPrice.mul(clothsols);
  }
  //Set up a gas
  function setGas(string oriderID,uint256 _gasvalue) public {
    if(isMaster(msg.sender)){
      orderGas[oriderID] = _gasvalue.mul(0.1);
    }
  }
}
