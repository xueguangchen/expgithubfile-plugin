package com.supporter.prj.view;

import com.supporter.prj.util.DateLabelFormatter;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import javax.swing.*;
import java.util.Properties;

/**
 * @author xueguangchen
 * @version 1.0.0
 * @ClassName com.supporter.prj.view.JDatePicker.java
 * @Description 自定义日期选择器类
 * @createTime 2024年12月03日 14:38:00
 */
public class JDatePicker extends JFormattedTextField {
    private final UtilDateModel model;
    private final JDatePanelImpl datePanel;
    private final JDatePickerImpl datePicker;

    public JDatePicker() {
        model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        datePanel = new JDatePanelImpl(model, p);
        datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
        datePicker.setBounds(0, 0, 180, 30);
        add(datePicker);
    }

    public UtilDateModel getModel() {
        return model;
    }
}
