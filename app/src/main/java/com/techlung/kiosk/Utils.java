package com.techlung.kiosk;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.techlung.kiosk.greendao.extended.ExtendedArticleDao;
import com.techlung.kiosk.greendao.extended.ExtendedCustomerDao;
import com.techlung.kiosk.greendao.extended.ExtendedPurchaseDao;
import com.techlung.kiosk.greendao.extended.KioskDaoFactory;
import com.techlung.kiosk.greendao.generated.Article;
import com.techlung.kiosk.greendao.generated.Customer;
import com.techlung.kiosk.greendao.generated.Purchase;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public final class Utils {

    public static boolean isAdmin = false;

    public static int convertDpToPixel(int dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = (int) Math.floor(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                resources.getDisplayMetrics()));
        return px;
    }

    public static int convertPixelsToDp(int px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int dp = (int) Math.floor(px / (metrics.densityDpi / 160f));
        return dp;
    }

    public static void clearPurchases(final Activity context) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Einkäufe löschen?");
        builder.setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.alert_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                KioskDaoFactory.getInstance(context).getExtendedPurchaseDao().deleteAll();
                dialog.dismiss();
                context.recreate();
            }
        });
        builder.show();
    }

    public static void doPayment(Activity activity) {
        KioskDaoFactory factory = KioskDaoFactory.getInstance(activity);
        ExtendedCustomerDao extendedCustomerDao = factory.getExtendedCustomerDao();
        ExtendedArticleDao extendedArticleDao = factory.getExtendedArticleDao();
        ExtendedPurchaseDao extendedPurchaseDao = factory.getExtendedPurchaseDao();


        DecimalFormat format = new DecimalFormat("0.00");

        for (Customer customer : extendedCustomerDao.getAllCustomers()) {
            try {

                StringBuffer shortSummary = new StringBuffer();
                StringBuffer entireSummary = new StringBuffer();

                List<Purchase> purchaseList = KioskDaoFactory.getInstance(activity).getExtendedPurchaseDao().getPurchaseByCustomer(customer.getId());
                float sum = 0f;
                StringBuffer purchaseSummary = new StringBuffer();
                for (Purchase purchase : purchaseList) {
                    if (purchase.getAmount() > 0) {
                        Article article = purchase.getArticle();
                        if (article == null) {
                            continue;
                        }
                        sum += purchase.getAmount() * article.getPrice();

                        purchaseSummary.append(article.getName() + " (" + format.format(article.getPrice()) + " " + activity.getString(R.string.sym_euro) + ")" + " x " + purchase.getAmount() + " = " + format.format(purchase.getAmount() * article.getPrice()) + " " + activity.getString(R.string.sym_euro) + "\n");
                    }
                }

                if (sum != 0) {

                    entireSummary.append("------------------------------------------------------------------\nZUSAMMENFASSUNG\n------------------------------------------------------------------\n");
                    entireSummary.append(purchaseSummary);
                    entireSummary.append("------------------------------------------------------------------\n" + "SUMME: " + format.format(sum) + " " + activity.getString(R.string.sym_euro));

                    shortSummary.append("Betrag: " + format.format(sum) + " " + activity.getString(R.string.sym_euro));

                    // create mail
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("text/plain");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{customer.getEmail()});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Kiosk Rechnung " + dateFormat.format(new Date()));

                    String emailBody =
                            "Hallo " + customer.getName() + "\n\n" + "vielen Dank für deinen Einkauf! Sag Bescheid falls irgendwas im Laden fehlt oder du irgendwelche speziellen Wünsche hast :). Deine Rechnung kannst du in Bar bei mir oder via PayPal begleichen: "
                                    + "\n\n" + shortSummary + "\n\n" + "https://www.paypal.me/OliverMetz/" + format.format(sum) + "\n\n" + entireSummary.toString()
                                    + "\n\n" + "Beste Grüße\nOliver";
                    emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);

                    activity.startActivity(emailIntent);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }


    }

    public static void initData(Context context) {
        KioskDaoFactory factory = KioskDaoFactory.getInstance(context);
        ExtendedCustomerDao extendedCustomerDao = factory.getExtendedCustomerDao();
        ExtendedArticleDao extendedArticleDao = factory.getExtendedArticleDao();

        if (extendedCustomerDao.getCount() == 0) {
            extendedCustomerDao.insertOrReplace(createCustomer("Thomas", "thomas.hanning@bertelsmann.de"));
            extendedCustomerDao.insertOrReplace(createCustomer("Robert", "robert.strickmann@bertelsmann.de"));
            extendedCustomerDao.insertOrReplace(createCustomer("Willem", "willem.terhoerst@bertelsmann.de"));
            extendedCustomerDao.insertOrReplace(createCustomer("Ilja", "ilja.wolik@bertelsmann.de"));
            extendedCustomerDao.insertOrReplace(createCustomer("Johannes", "johannes.kleeschulte@bertelsmann.de"));
            extendedCustomerDao.insertOrReplace(createCustomer("Jens", "jens.klingenberg@bertelsmann.de"));
        }

        if (extendedArticleDao.getCount() == 0) {
            extendedArticleDao.insertOrReplace(createArticle("Kinderriegel", 0.35f));
            extendedArticleDao.insertOrReplace(createArticle("Müsliriegel", 0.35f));
            extendedArticleDao.insertOrReplace(createArticle("Früchteriegel", 0.35f));
            extendedArticleDao.insertOrReplace(createArticle("Snickers", 0.45f));
            extendedArticleDao.insertOrReplace(createArticle("Balisto", 0.45f));
            extendedArticleDao.insertOrReplace(createArticle("Koppers", 0.45f));
            extendedArticleDao.insertOrReplace(createArticle("Twix", 0.45f));
            extendedArticleDao.insertOrReplace(createArticle("Pickup", 0.60f));
            extendedArticleDao.insertOrReplace(createArticle("Kitkat", 0.60f));
            extendedArticleDao.insertOrReplace(createArticle("Effect Energy", 1.10f));
            extendedArticleDao.insertOrReplace(createArticle("Cashews", 2.30f));
        }
    }

    private static Customer createCustomer(String name, String email) {
        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setName(name);
        return customer;
    }

    private static Article createArticle(String name, Float price) {
        Article article = new Article();
        article.setName(name);
        article.setPrice(price);
        return article;
    }
}
