package com.chat.wallet.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.chat.wallet.R;

public class PayPasswordDialog extends Dialog {
    private final View[] dots=new View[6];
    private final StringBuilder password=new StringBuilder();
    private OnPasswordCompleteListener listener;
    private String title,remark;
    public interface OnPasswordCompleteListener{void onComplete(String password);}
    public PayPasswordDialog(@NonNull Context ctx){super(ctx,android.R.style.Theme_Material_Light_Dialog_NoActionBar);}
    public void setTitle(String t){this.title=t;}
    public void setRemark(String r){this.remark=r;}
    public void setOnPasswordCompleteListener(OnPasswordCompleteListener l){this.listener=l;}

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);setContentView(R.layout.dialog_pay_password);
        Window w=getWindow();if(w!=null){w.setGravity(Gravity.BOTTOM);w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT);w.setWindowAnimations(android.R.style.Animation_InputMethod);}
        setCanceledOnTouchOutside(true);
        dots[0]=findViewById(R.id.dot1);dots[1]=findViewById(R.id.dot2);dots[2]=findViewById(R.id.dot3);
        dots[3]=findViewById(R.id.dot4);dots[4]=findViewById(R.id.dot5);dots[5]=findViewById(R.id.dot6);
        TextView titleTv=findViewById(R.id.titleTv);if(title!=null)titleTv.setText(title);
        TextView remarkTv=findViewById(R.id.remarkTv);if(remark!=null&&!remark.isEmpty()){remarkTv.setText(remark);remarkTv.setVisibility(View.VISIBLE);}
        ((ImageView)findViewById(R.id.closeIv)).setOnClickListener(v->dismiss());
        GridLayout g=findViewById(R.id.keyboardGrid);g.removeAllViews();
        int h=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,56,getContext().getResources().getDisplayMetrics());
        for(String k:new String[]{"1","2","3","4","5","6","7","8","9","","0","\u2190"}){
            TextView tv=new TextView(getContext());GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=h;p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
            tv.setLayoutParams(p);tv.setGravity(Gravity.CENTER);tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,22);tv.setTextColor(0xFF333333);tv.setText(k);
            tv.setBackgroundResource(android.R.drawable.list_selector_background);
            if(k.isEmpty()){tv.setEnabled(false);tv.setBackgroundColor(0xFFF5F5F5);}
            else if("\u2190".equals(k))tv.setOnClickListener(v->{if(password.length()>0){password.deleteCharAt(password.length()-1);upDots();}});
            else tv.setOnClickListener(v->{if(password.length()<6){password.append(k);upDots();if(password.length()==6&&listener!=null)listener.onComplete(password.toString());}});
            g.addView(tv);
        }
    }
    private void upDots(){for(int i=0;i<6;i++)dots[i].setVisibility(i<password.length()?View.VISIBLE:View.INVISIBLE);}
    public void clearPassword(){password.setLength(0);upDots();}
}
