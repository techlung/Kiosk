package com.techlung.kiosk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pixplicity.easyprefs.library.Prefs;
import com.techlung.kiosk.generic.BaseActivity;
import com.techlung.kiosk.model.Customer;
import com.techlung.kiosk.utils.Utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.Sort;

public class CustomerActivity extends BaseActivity {

    private List<Customer> customers = new ArrayList<>();
    private ArrayAdapter<Customer> adapter;

    private int adminCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        new Prefs.Builder()
                .setContext(this)
                .setMode(ContextWrapper.MODE_PRIVATE)
                .setPrefsName(this.getPackageName())
                .setUseDefaultSharedPreference(true)
                .build();

        Button addCustomer = (Button) findViewById(R.id.addCustomer);
        addCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editCustomer(null, true);
            }
        });

        ListView customerListView = (ListView) findViewById(R.id.customerList);
        adapter = new CustomerAdapter(this, R.layout.customer_list_item, customers);
        customerListView.setAdapter(adapter);

        customerListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                editCustomer(customers.get(position), false);
                return true;
            }
        });

        customerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Customer customer = customers.get(position);
                Intent intent = new Intent(CustomerActivity.this, ArticleActivity.class);
                intent.putExtra(ArticleActivity.CUSTOMER_ID_EXTRA, customer.getId());
                startActivity(intent);
            }
        });

        updateUi();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (Utils.isAdmin) {
            getMenuInflater().inflate(R.menu.customer_menu, menu);
            return true;
        }

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
            adminCounter++;
            if (adminCounter > 5) {
                adminCounter = 0;
            }
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (adminCounter >= 5) {
                adminCounter++;
            } else {
                adminCounter = 0;
            }
            if (adminCounter == 10) {
                adminCounter = 0;
                Utils.isAdmin = !Utils.isAdmin;

                if (Utils.isAdmin) {
                    Toast.makeText(this, "Admin Mode", Toast.LENGTH_SHORT).show();
                }

                this.recreate();

            }
        }

        Log.d("AdminCounter", "" + adminCounter);

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.pay) {
            Utils.doPayment(this);
            return true;
        } else if (id == R.id.restore) {
            Utils.clearPurchases(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateUi() {
        Customer.updateRanking();

        customers.clear();
        customers.addAll(Realm.getDefaultInstance().where(Customer.class).findAllSorted("rank", Sort.ASCENDING));

        adapter.notifyDataSetChanged();
    }

    private void editCustomer(@Nullable final Customer customer, boolean createNew) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (createNew) {
            builder.setTitle(R.string.customer_create);
        } else {
            builder.setTitle(R.string.customer_edit);
        }

        View content = LayoutInflater.from(this).inflate(R.layout.customer_add, null, false);

        final EditText name = (EditText) content.findViewById(R.id.input_name);
        if (customer != null) {
            name.setText(customer.getName());
        }

        final EditText email = (EditText) content.findViewById(R.id.input_email);
        if (customer != null) {
            email.setText(customer.getEmail());
        }

        builder.setView(content);

        builder.setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Customer customerNew = customer;

                Realm.getDefaultInstance().beginTransaction();
                if (customerNew == null) {
                    customerNew = Realm.getDefaultInstance().createObject(Customer.class, Customer.createId());
                }
                customerNew.setName(name.getText().toString());
                customerNew.setEmail(email.getText().toString());
                Realm.getDefaultInstance().commitTransaction();

                updateUi();
            }
        });

        builder.setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        if (!createNew && Utils.isAdmin) {
            builder.setNeutralButton(R.string.alert_delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (customer != null) {
                        Realm.getDefaultInstance().beginTransaction();
                        customer.deleteFromRealm();
                        Realm.getDefaultInstance().commitTransaction();
                    }

                    updateUi();
                }
            });
        }

        builder.show();

    }

    private class CustomerAdapter extends ArrayAdapter<Customer> {
        CustomerAdapter(Context context, @LayoutRes int resource, @NonNull List<Customer> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.customer_list_item, parent, false);
            }

            TextView name = (TextView) convertView.findViewById(R.id.customer_name);
            TextView dept = (TextView) convertView.findViewById(R.id.customer_dept);

            Customer customer = getItem(position);

            name.setText(customer.getName());

            DecimalFormat format = new DecimalFormat("0.00");
            dept.setText(format.format(customer.getPurchaseValueSum()) + " " + getString(R.string.sym_euro));

            dept.setVisibility(View.VISIBLE);

            return convertView;
        }
    }

}
