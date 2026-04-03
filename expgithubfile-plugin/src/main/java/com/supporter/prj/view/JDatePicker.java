package com.supporter.prj.view;

import com.supporter.prj.util.DateLabelFormatter;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;
import java.util.Date;

/**
 * @author xueguangchen
 * @version 1.0.0
 * @ClassName com.supporter.prj.view.JDatePicker.java
 * @Description 自定义日期选择器类
 * @createTime 2024年12月03日 14:38:00
 */
public class JDatePicker extends JPanel {
    private final UtilDateModel model;
    private final JDatePickerImpl datePicker;

    public JDatePicker() {
        setLayout(new BorderLayout());
        model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "今天");
        p.put("text.month", "月");
        p.put("text.year", "年");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
        add(datePicker, BorderLayout.CENTER);
    }

    public UtilDateModel getModel() {
        return model;
    }
    
    public Date getValue() {
        return (Date) model.getValue();
    }
    
    public void setValue(Date date) {
        if (date != null) {
            model.setValue(date);
        }
    }
}
