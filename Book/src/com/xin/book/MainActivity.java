package com.xin.book;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;

import com.xin.book.view.BookAdapter;
import com.xin.book.view.BookLayout;

public class MainActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	 BookLayout bk = new BookLayout(this);
         List<String> str = new ArrayList<String>();
         str.add("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
         str.add("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
         str.add("ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
         str.add("ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd");
         BookAdapter ba = new BookAdapter(this);
         ba.addItem(str);
         bk.setPageAdapter(ba);
         setContentView(bk);
    }
}