package com.anweshainfo.anwesha_registration;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.anweshainfo.anwesha_registration.Adapter.CustomSpinnerAdapter;
import com.anweshainfo.anwesha_registration.Adapter.RVAdapter;
import com.anweshainfo.anwesha_registration.model.Participant;
import com.google.zxing.Result;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.dm7.barcodescanner.zxing.ZXingScannerView;


/**
 * Created by manish on 27/10/17.
 */

public class qrscannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    RequestQueue mQueue;
    @BindView(R.id.scanner)
    LinearLayout scannerView;
    @BindView(R.id.spinner_events_name)
    Spinner eventsspinner;
    @BindView(R.id.rv_participants)
    RecyclerView recyclerView;
    private SharedPreferences.Editor isLogged;
    private ZXingScannerView mScannerView;
    private ArrayAdapter<String> spinnerArrayAdapter;
    private ArrayList<String> string = new ArrayList<>();
    private ArrayList<String> id = new ArrayList<>();
    private ArrayList<Participant> participants = new ArrayList<>();
    private String mBaseUrl;
    private SharedPreferences mSharedPreferences;
    private RVAdapter rvAdapter;
    private String eventName;
    private String eventId;
    private boolean isPaymentReg = false;
    private boolean iseveReg = false;
    private boolean isViewReq = false;
    private String paymentRegId = "0";
    private String viewUserId = "view";
    private String mMakepaymentUrl;
    private CustomSpinnerAdapter customSpinnerAdapter;
    private JSONObject jsonObject;
    private boolean isCamActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_selector);
        setUI();
    }

    private void setUI() {
        ButterKnife.bind(this);
        mBaseUrl = getResources().getString(R.string.url_register);
        //mMakepaymentUrl=getResources().getString(R.string.makePaymentUrl);
        Log.e("This ", "This activity was started .....");
        Log.e("Thissss", "" + string.size());
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        isLogged = PreferenceManager.getDefaultSharedPreferences(this).edit();
        mQueue = Volley.newRequestQueue(this);

        mScannerView = new ZXingScannerView(this);
        mScannerView.setAutoFocus(true);

        checkPermission();

        //extracting the json response
        String response = mSharedPreferences.getString("jsonResponse", "");
        try {
            jsonObject = new JSONObject(response);
        } catch (Exception e) {
            Log.e("Error in Json", e.toString());
        }
        string = filterEventName(jsonObject);
        string.add(getString(R.string.view_user_details));

        id = filterEventid(jsonObject);
        id.add(viewUserId);

        eventId = id.get(0);

        //set the array adapter
        customSpinnerAdapter = new CustomSpinnerAdapter(this, string);
        eventsspinner.setAdapter(customSpinnerAdapter);

        eventsspinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                show(i);
                //setting the value
                eventName = string.get(i);
                eventId = id.get(i);
                if (eventId.equals(paymentRegId)) {
                    isPaymentReg = true;
                    iseveReg = false;
                    isViewReq = false;
                } else if (eventId.equals(viewUserId)) {
                    isPaymentReg = false;
                    iseveReg = false;
                    isViewReq = true;
                } else {
                    iseveReg = true;
                    isPaymentReg = false;
                    isViewReq = false;
                }
                setUpRV();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        scannerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Start the scan
                Scan();
            }
        });

        rvAdapter = new RVAdapter(participants);
        recyclerView.setAdapter(rvAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        setUpRV();
    }

    private void setUpRV() {
        participants.clear();
        rvAdapter.notifyDataSetChanged();
        String postUrl = "https://www.anwesha.info/events/getReg/" + eventId + "/";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, postUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v("ResponseX:", response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            int status = jsonObject.getInt("status");
                            if (status == 1) {
                                fillRV(jsonObject);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.v("Error : ", error.toString());
                        error.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Error logging in. Please try again later", Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("userID", getuID().substring(3));
                Log.e("USERID : ", getuID().substring(3));
                Log.e("AUTHKEY : ", mSharedPreferences.getString("key", ""));
                params.put("authKey", mSharedPreferences.getString("key", ""));
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        mQueue.add(stringRequest);
    }

    private void fillRV(JSONObject jsonObject) throws JSONException {
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        participants.clear();
        Log.e("REsponseX1",jsonArray.length()+"") ;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject j1 = jsonArray.getJSONObject(i);
            participants.add(new Participant(j1.getString("name"), j1.getString("pId")));
        }
        rvAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out:
                Toast.makeText(this, "Logging Out", Toast.LENGTH_LONG).show();
                isLogged.putBoolean("isloggedIn", false);
                isLogged.apply();
                isLogged.commit();
                Intent intent = new Intent(qrscannerActivity.this, MainActivity.class);
                finish();
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("savedInstanceEvent", string);
        outState.putStringArrayList("savedInstanceID", id);

    }

    private ArrayList<String> filterEventName(JSONObject jsonObject) {
        try {
            JSONObject special = jsonObject.getJSONObject("special");
            JSONObject eventOrganizer = special.getJSONObject("eventOrganiser");
            //getting the number of count of events
            int count = eventOrganizer.getInt("eveCount");
            String name;
            ArrayList<String> eventName = new ArrayList<>();


            //filling the arraylist
            for (int i = 0; i < count; ++i) {
                JSONObject events = eventOrganizer.getJSONObject("" + i);
                name = events.getString("name");
                eventName.add(name);
            }
            return eventName;

        } catch (JSONException e) {
            Log.e("Mainactivity.class ", " Error in parsing json event " + e.getMessage());
        }
        return null;
    }

    /**
     * Helper method to show the value which is selected
     */
    public void show(int i) {
        Toast.makeText(this, string.get(i), Toast.LENGTH_LONG).show();
    }

    public void Scan() {
        isCamActive = true;
        setContentView(mScannerView);
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
        mScannerView.setAutoFocus(true);

    }

    @Override
    public void onBackPressed() {
        if (isCamActive) {
            mScannerView.stopCamera();
            isCamActive = false;
//            mScannerView = null;
//            this.finish();
//            Intent intent = new Intent(this, qrscannerActivity.class);
//            startActivity(intent);
            setContentView(R.layout.event_selector);
            setUI();
        } else
            super.onBackPressed();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    protected void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();

    }

    @Override
    public void handleResult(Result rawResult) {
        // Do something with the result here
        Log.v("TAG", rawResult.getText()); // Prints scan results
        Log.v("TAG", rawResult.getBarcodeFormat().toString()); // Prints the scan format (qrcode, pdf417 etc.)

        if (iseveReg) {
            //appending the base url
            String postUrl = mBaseUrl + rawResult.getText();
            //make a network call
            makePost(postUrl);
        } else if (isPaymentReg) {
            //launch a new intent to make payment
            makePaymentReg(rawResult.getText(), false);
        } else if (isViewReq) {
            //launch a new intent to view user
            makePaymentReg(rawResult.getText(), true);
        }


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        // starts the scanning back up again
                        mScannerView.resumeCameraPreview(qrscannerActivity.this);
                    }
                });
            }
        }).start();

    }

    //check whether there is permission
    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            //We don't need an explanation because this will definitely require camera access to scan
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this, "This is done", Toast.LENGTH_LONG).show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Error");
                    builder.setMessage("Unable to Start camera.\n Go to Settings\n->App->Anwesha2k17-Registration->Permission\n" +
                            "Turn on Camera there");
                    AlertDialog alert1 = builder.create();
                    alert1.show();
                    this.finish();
                }
            }
        }
    }


    private void makePost(String postUrl) {

        StringRequest stringRequest = new StringRequest(Request.Method.POST, postUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v("Response:", response);

                        try {

                            JSONObject jsonObject = new JSONObject(response);
                            int status = jsonObject.getInt("http");

                            switch (status) {
                                case 200:

                                    Toast.makeText(getApplicationContext(), "Scan successful", Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(qrscannerActivity.this, reg_result.class);
                                    startActivity(intent);
                                    break;
                                case 400:
                                    Toast.makeText(getApplicationContext(), "Invalid Anwesha Id", Toast.LENGTH_SHORT).show();
                                    break;
                                case 409:
                                    Toast.makeText(getApplicationContext(),"This ID is already registered", Toast.LENGTH_LONG).show();

                                    break;
                                case 403:
                                    Toast.makeText(getApplicationContext(), "Invalid Id", Toast.LENGTH_LONG).show();

                                    break;
                                default:
                                    Toast.makeText(getApplicationContext(), "Error .Please try again later", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.v("Error : ", error.toString());
                        error.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Error logging in. Please try again later", Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put(getString(R.string.event_id), eventId);
                params.put("authKey", mSharedPreferences.getString("key", ""));
                params.put("orgID", getuID().substring(3));
                Log.e("USERID : ", getuID().substring(3));
                Log.e("AUTHKEY : ", mSharedPreferences.getString("key", ""));
                params.put("authKey", mSharedPreferences.getString("key", ""));

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        mQueue.add(stringRequest);

    }

    /**
     * @params result returns the uID(ANWESHAID) of the
     * the id which is anwesha ID registration of id
     */

    private String getuID() {
        String uID = mSharedPreferences.getString("uID", null);
        return uID;
    }

    /**
     * @value takes the value of qrcode hashed key
     * function launches a new intent with the values of
     * string with the response
     */
    private void makePaymentReg(String value, final boolean viewOnly) {
        String requestUrl = mBaseUrl + value;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, requestUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v("Response:", response);

                        try {

                            JSONObject jsonObject = new JSONObject(response);
                            int status = jsonObject.getInt("http");

                            switch (status) {
                                case 200:
                                Intent intent = new Intent(qrscannerActivity.this, payment_activity.class);
                                intent.putExtra("jsonresponse", response);
                                intent.putExtra("viewOnly", viewOnly);
                            startActivity(intent);
                        // Display the first 500 characters of the response string.
                                    break;
                                case 400:
                                    Toast.makeText(getApplicationContext(), "Invalid Email Id", Toast.LENGTH_SHORT).show();
                                    break;
                                case 409:
                                    Toast.makeText(getApplicationContext(), R.string.message_registration_duplicate, Toast.LENGTH_LONG).show();

                                    break;
                                case 403:
                                    Toast.makeText(getApplicationContext(), "Invalid Login", Toast.LENGTH_LONG).show();

                                    break;
                                default:
                                    Toast.makeText(getApplicationContext(), "Error logging in. Please try again later", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.v("Error : ", error.toString());
                        error.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Error logging in. Please try again later", Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("authKey", mSharedPreferences.getString("key", ""));
                params.put("orgID", getuID().substring(3));
                Log.e("USERID : ", getuID().substring(3));
                Log.e("AUTHKEY : ", mSharedPreferences.getString("key", ""));
                params.put("authKey", mSharedPreferences.getString("key", ""));

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        mQueue.add(stringRequest);







//        // Request a string response from the provided URL.
//        StringRequest stringRequest = new StringRequest(Request.Method.POST, requestUrl,
//                new Response.Listener<String>() {
//                    @Override
//                    public void onResponse(String response) {
//
//                        Log.e("TAG Volley", response);
//                        //After getting the response put it in string and start the activity
//                        Intent intent = new Intent(qrscannerActivity.this, payment_activity.class);
//                        intent.putExtra("jsonresponse", response);
//                        intent.putExtra("viewOnly", viewOnly);
//                        startActivity(intent);
//                        // Display the first 500 characters of the response string.
//
//                    }
//                }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Log.e("TAG", error.getMessage());
//            }
//        });
//        // Add the request to the RequestQueue.
//        mQueue.add(stringRequest);
    }



    private ArrayList<String> filterEventid(JSONObject jsonObject) {
        try {
            JSONObject special = jsonObject.getJSONObject("special");
            JSONObject eventOrganizer = special.getJSONObject("eventOrganiser");
            //getting the number of count of events
            int count = eventOrganizer.getInt("eveCount");
            String id;
            ArrayList<String> eventId = new ArrayList<>();
            Log.e("dhjfhfdfjjjjjj", "this sis ssijfjsds sd ssfsf fsf" + count);
            //filling the arraylist
            for (int i = 0; i < count; ++i) {
                JSONObject events = eventOrganizer.getJSONObject("" + i);
                id = events.getString("id");
                eventId.add(id);
            }

            return eventId;

        } catch (JSONException e) {
            Log.e("Mainactivity.class ", " Error in parsing json id " + e.getMessage());
        }

        return null;
    }

}