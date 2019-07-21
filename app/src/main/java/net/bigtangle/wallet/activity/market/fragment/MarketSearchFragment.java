package net.bigtangle.wallet.activity.market.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;

import net.bigtangle.core.Coin;
import net.bigtangle.core.ECKey;
import net.bigtangle.core.Json;
import net.bigtangle.core.NetworkParameters;
import net.bigtangle.core.OrderRecord;
import net.bigtangle.core.http.server.resp.OrderdataResponse;
import net.bigtangle.params.ReqCmd;
import net.bigtangle.wallet.R;
import net.bigtangle.wallet.activity.market.adapter.MarketOrderItemListAdapter;
import net.bigtangle.wallet.activity.market.model.MarketOrderItem;
import net.bigtangle.wallet.components.WrapContentLinearLayoutManager;
import net.bigtangle.wallet.core.WalletContextHolder;
import net.bigtangle.wallet.core.http.HttpNetComplete;
import net.bigtangle.wallet.core.http.HttpNetTaskRequest;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author lijian
 * @date 2019-07-06 00:06:01
 */
public class MarketSearchFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    @BindView(R.id.address_text_input)
    TextInputEditText addressTextInput;

    @BindView(R.id.state_radio_group)
    RadioGroup stateRadioGroup;

    @BindView(R.id.only_me_switch)
    Switch onlyMeSwitch;

    @BindView(R.id.search_button)
    Button searchButton;

    @BindView(R.id.recyclerViewContainer)
    RecyclerView mRecyclerView;

    @BindView(R.id.swipeContainer)
    SwipeRefreshLayout mSwipeRefreshLayout;

    private List<MarketOrderItem> itemList;

    private MarketOrderItemListAdapter mAdapter;

    public MarketSearchFragment() {
    }

    public static MarketSearchFragment newInstance() {
        MarketSearchFragment fragment = new MarketSearchFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.itemList == null) {
            this.itemList = new ArrayList<MarketOrderItem>();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_market_search, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.mAdapter = new MarketOrderItemListAdapter(getContext(), itemList);

        mSwipeRefreshLayout.setOnRefreshListener(this);

        LinearLayoutManager layoutManager = new WrapContentLinearLayoutManager(getContext());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mAdapter);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initData();
            }
        });

        initData();
    }

    private void initData() {
        String state = "";
        for (int i = 0; i < stateRadioGroup.getChildCount(); i++) {
            RadioButton radioButton = (RadioButton) stateRadioGroup.getChildAt(i);
            if (radioButton.isChecked()) {
                state = radioButton.getText().equals(getContext().getString(R.string.publish)) ? "publish" : "match";
                break;
            }
        }

        HashMap<String, Object> requestParam = new HashMap<String, Object>();
        requestParam.put("address", addressTextInput.getText().toString());
        requestParam.put("state", state);
        requestParam.put("spent", "publish".equals(state) ? "false" : "true");
        if (onlyMeSwitch.isChecked()) {
            List<ECKey> walletKeys = WalletContextHolder.get().wallet().walletKeys(WalletContextHolder.getAesKey());
            List<String> address = new ArrayList<String>();
            for (ECKey ecKey : walletKeys) {
                address.add(ecKey.toAddress(WalletContextHolder.networkParameters).toString());
            }
            requestParam.put("addresses", address);
        }

        new HttpNetTaskRequest(this.getContext()).httpRequest(ReqCmd.getOrders, requestParam, new HttpNetComplete() {
            @Override
            public void completeCallback(String jsonStr) {
                try {
                    OrderdataResponse orderdataResponse = Json.jsonmapper().readValue(jsonStr, OrderdataResponse.class);
                    itemList.clear();
                    for (OrderRecord orderRecord : orderdataResponse.getAllOrdersSorted()) {
                        MarketOrderItem marketOrderItem = new MarketOrderItem();
                        if (NetworkParameters.BIGTANGLE_TOKENID_STRING.equals(orderRecord.getOfferTokenid())) {
                            marketOrderItem.setType("BUY");
                            marketOrderItem.setAmount(orderRecord.getTargetValue());
                            marketOrderItem.setTokenId(orderRecord.getTargetTokenid());
                            marketOrderItem.setPrice(Coin.toPlainString(orderRecord.getOfferValue() / orderRecord.getTargetValue()));
                        } else {
                            marketOrderItem.setType("SELL");
                            marketOrderItem.setAmount(orderRecord.getOfferValue());
                            marketOrderItem.setTokenId(orderRecord.getOfferTokenid());
                            marketOrderItem.setPrice(Coin.toPlainString(orderRecord.getTargetValue() / orderRecord.getOfferValue()));
                        }
                        marketOrderItem.setOrderId(orderRecord.getInitialBlockHashHex());
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                        marketOrderItem.setValidateTo(dateFormat.format(new Date(orderRecord.getValidToTime() * 1000)));
                        marketOrderItem.setValidateFrom(dateFormat.format(new Date(orderRecord.getValidFromTime() * 1000)));
                        marketOrderItem.setAddress(ECKey.fromPublicOnly(orderRecord.getBeneficiaryPubKey()).toAddress(WalletContextHolder.networkParameters).toString());
                        marketOrderItem.setInitialBlockHashHex(orderRecord.getInitialBlockHashHex());
                        itemList.add(marketOrderItem);

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onRefresh() {
        this.initData();
        mSwipeRefreshLayout.setRefreshing(false);
        mAdapter.notifyDataSetChanged();
    }
}
