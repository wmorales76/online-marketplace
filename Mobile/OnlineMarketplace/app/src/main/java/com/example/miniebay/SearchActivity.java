package com.example.miniebay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

import static android.content.Context.MODE_PRIVATE;

public class SearchActivity extends AppCompatActivity {
    SharedPreferences prf;

    //This is for debugging
    private String TAG = SearchActivity.class.getSimpleName();

    //This is for managing the listview in the activity
    private ListView lv;

    private EditText search;
    private Spinner deptList;
    //Web server's IP address
    private String hostAddress;

    //Users adapter
    private UsersAdapter adapter;

    // Item list for storing data from the web server
    private ArrayList<userItem> itemUserList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);
        //Link activity's controls with Java variables
        Button btnLogOut = (Button)findViewById(R.id.btnLogOut);
        Button btnSearch = (Button)findViewById(R.id.btnSearch);
        search = (EditText)findViewById(R.id.searchInput);
        deptList = (Spinner) findViewById(R.id.deptList);
        //access the local session variables
        prf = getSharedPreferences("user_details",MODE_PRIVATE);

        //Display on the screen

        // Define the web server's IP address
        hostAddress="192.168.0.215:8080";
        //hostAddress="10.72.126.188:8088";
        //Instate the Item list
        itemUserList = new ArrayList<>();

        // Defines the adapter: Receives the context (Current activity) and the Arraylist
        adapter = new UsersAdapter(this, itemUserList);

        // Create a accessor to the ListView in the activity
        lv = (ListView) findViewById(R.id.itemList);

        // Create and start the thread
        new GetItems(this).execute();
        new GetDepartments(this).execute();

        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Logout
                //Destroy local session variables
                SharedPreferences.Editor editor = prf.edit();
                editor.clear();
                editor.commit();

                // finish the activity as well as all the below Activities in the execution stack.
                SearchActivity.this.finishAffinity(); // supported from API 16

                //call the MainActivity for login
                Intent intent = new Intent(SearchActivity.this,SearchActivity.class);
                startActivity(intent);
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Search
                //Destroy local session variables
                SharedPreferences.Editor editor = prf.edit();
                editor.putString("search",search.getText().toString());
                editor.putString("dept",deptList.getSelectedItem().toString());
                editor.commit();
                // finish the activity as well as all the below Activities in the execution stack.

                //call the MainActivity for login
                Intent intent = new Intent(SearchActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });
    }

    /***
     *  This class is a thread for receiving and process data from the Web server
     */
    private class GetItems extends AsyncTask<Void, Void, Void> {

        // Context: every transaction in a Android application must be attached to a context
        private Activity activity;

        private Drawable actualBaseImage;

        /***
         * Special constructor: assigns the context to the thread
         *
         * @param activity: Context
         */
        //@Override
        protected GetItems(Activity activity)
        {
            this.activity = activity;
        }

        /**
         *  on PreExecute method: runs after the constructor is called and before the thread runs
         */
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(SearchActivity.this, "Items list is downloading", Toast.LENGTH_LONG).show();
        }

        /***
         *  Main thread
         * @param arg0
         * @return
         */
        protected Void doInBackground(Void... arg0) {
            //Create a HttpHandler object
            HttpHandler sh = new HttpHandler();
            // Making a request to url and getting response
            String url = "http://"+hostAddress+"/searchServlet";

            String search = prf.getString("search",null);
            String deptName = prf.getString("dept",null);

            Log.e(TAG, "Search: " + search);
            Log.e(TAG, "Dept: " + deptName);
            // Download data from the web server using JSON;
            String jsonStr = sh.makeSearchServiceCall(url, search, deptName);

            // Log download's results
            Log.e(TAG, "Response from url: " + jsonStr);

            //The JSON data must contain an array of JSON objects
            if (jsonStr != null) {
                try {
                    //Define a JSON object from the received data
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    JSONArray items = jsonObj.getJSONArray("searchItems");

                    // looping through All Items
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject c = items.getJSONObject(i);

                        String product_id = c.getString("product_id");
                        String sellerUserName = c.getString("sellerUserName");
                        String productName = c.getString("productName");
                        String productDescription = c.getString("productDescription");
                        String startingBid = c.getString("startingBid");
                        String dueDate = c.getString("dueDate");
                        String department = c.getString("deptName");
                        String imageLocation = c.getString("imagePath");



                        //Create URL for each image
                        String imageURL = "http://" + hostAddress + "/cpen410/imagesjson/" + imageLocation;
                        //Download the actual image using the imageURL
                        Drawable actualImage= LoadImageFromWebOperations(imageURL);

                        // Create an userItem object and add it to the items' list
                        //itemUserList.add(new userItem(department, building, actualImage));
                        itemUserList.add(new userItem(product_id,sellerUserName,productName,productDescription,startingBid,dueDate,department,actualImage));
                    }
                } //Log any problem with received data
                catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show(); }
                    });
                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server. Check LogCat for possible errors!",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
            return null;
        }

        /***
         *  This method runs after thread completion
         *  Set up the List view using the ArrayAdapter
         *
         * @param result
         */
        protected void onPostExecute (Void result){
            super.onPostExecute(result);
            lv.setAdapter(adapter);
        }

        /***
         *  This method downloads a image from a web server using an URL
         * @param url: Image URL
         * @return  d: android.graphics.drawable.Drawable;
         * */
        public Drawable LoadImageFromWebOperations(String url) {
            try {
                //Request the image to the web server
                InputStream is = (InputStream) new URL(url).getContent();

                //Generates an android.graphics.drawable.Drawable object
                Drawable d = Drawable.createFromStream(is, "src name");

                return d; }
            catch (Exception e) {
                return null;
            }
        }
    }

    private class GetDepartments extends AsyncTask<Void, Void, List<String>> {

        // Context: every transaction in a Android application must be attached to a context
        private Activity activity;

        /***
         * Special constructor: assigns the context to the thread
         *
         * @param activity: Context
         */
        //@Override
        protected GetDepartments(Activity activity)
        {
            this.activity = activity;
        }

        /**
         *  on PreExecute method: runs after the constructor is called and before the thread runs
         */
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(SearchActivity.this, "Retrieving department list", Toast.LENGTH_LONG).show();
        }

        /***
         *  Main thread
         * @param arg0
         * @return
         */
        protected List<String> doInBackground(Void... arg0) {
            //Create a HttpHandler object
            HttpHandler sh = new HttpHandler();
            // Making a request to url and getting response
            String url = "http://"+hostAddress+"/getDepartments";
            // Download data from the web server using JSON;
            String jsonStr = sh.makeServiceCall(url);
            ArrayList<String> itemList = new ArrayList<>();
            // Log download's results
            Log.e(TAG, "Response from url: " + jsonStr);

            //The JSON data must contain an array of JSON objects
            if (jsonStr != null) {
                try {
                    //Define a JSON object from the received data
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    JSONArray items = jsonObj.getJSONArray("departments");

                    // looping through All Items
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject c = items.getJSONObject(i);
                        // Get the item name and add it to the array
                        String itemName = c.getString("deptName");
                        itemList.add(itemName);
                    }
                    //Create an ArrayAdapter for the AutoCompleteTextView
                }catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show(); }
                    });
                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server. Check LogCat for possible errors!",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
            return itemList;
        }

        /***
         *  This method runs after thread completion
         *  Set up the List view using the ArrayAdapter
         *
         * @param result
         */
        protected void onPostExecute(List<String> result) {
            super.onPostExecute(result);
            result.add(0, "All");
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_dropdown_item, result);
            deptList.setAdapter(adapter);
            // Set the Spinner to the first position programmatically
            deptList.setSelection(0);

            deptList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedDept = (String) parent.getItemAtPosition(position);
                    if (!selectedDept.equals("All")) {
                        Toast.makeText(getApplicationContext(), "Selected: " + selectedDept, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Selected: All Departments", Toast.LENGTH_LONG).show();
                    }
                    // Add any action you want to take after selecting an item
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

        /***
         *  This method downloads a image from a web server using an URL
         * @param url: Image URL
         * @return  d: android.graphics.drawable.Drawable;
         * */
        public Drawable LoadImageFromWebOperations(String url) {
            try {
                //Request the image to the web server
                InputStream is = (InputStream) new URL(url).getContent();

                //Generates an android.graphics.drawable.Drawable object
                Drawable d = Drawable.createFromStream(is, "src name");

                return d; }
            catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * This class defines a ArrayAdapter for the ListView manipulation
     */
    public class UsersAdapter extends ArrayAdapter<userItem> {

        /**
         *  Constructor:
         * @param context: Activity
         * @param users: ArrayList for storing Items list
         */
        public UsersAdapter(Context context, ArrayList<userItem> users) {
            super(context, 0, users);
        }

        /***
         *  This method generates a view for manipulating the item list
         *  This method is called from the ListView.
         *
         * @param position: Item's position in the ArrayList
         * @param convertView:
         * @param parent
         * @return
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            userItem user = getItem(position);

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
            }

            // Lookup views for data population
            TextView productName = (TextView) convertView.findViewById(R.id.productName);
            TextView dueDate = (TextView) convertView.findViewById(R.id.dueDate);
            TextView department = (TextView) convertView.findViewById(R.id.department);
            ImageView productImage = (ImageView) convertView.findViewById(R.id.imageView);
            TextView price = (TextView) convertView.findViewById(R.id.price);

            // Populate the data into the template view using the data object
            productName.setText(user.productName);
            dueDate.setText("Due Date: " + user.dueDate);
            department.setText("Dept: " + user.department);
            price.setText("Price: " + user.startingBid);
            productImage.setImageDrawable(user.image);

            // Return the completed view to render on screen
            convertView.setTag(position);

            // Create Listener to detect a click
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = (Integer) view.getTag();
                    Toast.makeText(getContext(),
                            "You selected " + itemUserList.get(position).productName,
                            Toast.LENGTH_LONG).show();
                    // Additional actions can be added here
                    prf.edit().putString("productID", itemUserList.get(position).productID).apply();
                    Intent intent = new Intent(SearchActivity.this, ProductActivity.class);
                    startActivity(intent);

                }
            });
            return convertView;
        }


    }

    /**
     *  This class generates a Data structure for manipulating each Item in the application
     */
    public class userItem {
        public String productID;
        public String sellerUserName;
        public String productName;
        public String productDescription;
        public String startingBid;
        public String dueDate;
        public String department;
        public Drawable image;

        public userItem(String productID, String sellerUserName, String productName, String productDescription, String startingBid, String dueDate, String department, Drawable image) {
            this.productID = productID;
            this.sellerUserName = sellerUserName;
            this.productName = productName;
            this.productDescription = productDescription;
            this.startingBid = startingBid;
            this.dueDate = dueDate;
            this.department = department;
            this.image = image;
        }

        public String getProductID() {
            return productID;
        }

        public String getSellerUserName() {
            return sellerUserName;
        }

        public String getProductName() {
            return productName;
        }

        public String getProductDescription() {
            return productDescription;
        }

        public String getStartingBid() {
            return startingBid;
        }

        public String getDueDate() {
            return dueDate;
        }

        public String getDeptName() {
            return department;
        }

        public Drawable getImagePath() {
            return image;
        }


    }

}