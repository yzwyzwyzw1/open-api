import eth from '../utils/eth';
import { SET_CURRENT_ETH_ACCOUNT } from './types';
import { fetchScaffoldDetails } from './scaffolds';
import { openTokenSelector } from '../selectors/open-token';
import { apiPost } from './apiRequest';
import { getScaffoldDeactivateScaffoldPath } from '../utils/apiPathes';

export const activateScaffold = (scaffoldAddress, fromAddress, amount = '10.0') => async (dispatch, getState) => {
  const openToken = openTokenSelector(getState());
  const hash = await openToken.transfer(scaffoldAddress, amount * 100000000, { from: fromAddress });

  dispatch({ type: SET_CURRENT_ETH_ACCOUNT, payload: { activating: true, activatingHash: hash } });
  dispatch(subscribeScaffoldActivation(hash, scaffoldAddress));
};

export const deactivateScaffold = (scaffoldAddress, abi, developerAddress) => async dispatch => {
  const scaffoldContract = eth.contract(JSON.parse(abi)).at(scaffoldAddress);
  const hash = await scaffoldContract.deactivate({ from: developerAddress });

  dispatch({ type: SET_CURRENT_ETH_ACCOUNT, payload: { activating: true, activatingHash: hash } });
  dispatch(subscribeScaffoldActivation(hash, scaffoldAddress));
};

export const deactivateScaffoldByApi = scaffoldAddress => async dispatch => {
  try {
    await dispatch(apiPost(getScaffoldDeactivateScaffoldPath()));
    dispatch(fetchScaffoldDetails(scaffoldAddress));
  } catch (err) {
    console.log('Error deactivating scaffolds', err);
  }
};

let interval;

export const subscribeScaffoldActivation = (hash, scaffoldAddress) => async dispatch => {
  if (!hash) {
    dispatch({ type: SET_CURRENT_ETH_ACCOUNT, payload: { activating: false, activatingHash: null } });
  }

  if (interval || !hash) {
    return;
  }

  interval = setInterval(async () => {
    try {
      const receipt = await eth.getTransactionReceipt(hash);
      if (!receipt) return;
      clearInterval(interval);
      interval = null;
      dispatch({ type: SET_CURRENT_ETH_ACCOUNT, payload: { activating: false, activatingHash: null } });
      dispatch(fetchScaffoldDetails(scaffoldAddress));
    } catch (reason) {
      clearInterval(interval);
    }
  }, 1000);
};
