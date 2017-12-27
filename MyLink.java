package sample;

import javafx.beans.property.SimpleStringProperty;

public class MyLink {
    private final SimpleStringProperty address;
    private final SimpleStringProperty date;

    public MyLink(String add) {
        this.address = new SimpleStringProperty(add);
        this.date = new SimpleStringProperty("");
    }

    public MyLink(String add, String dat) {
        this.address = new SimpleStringProperty(add);
        this.date = new SimpleStringProperty(dat);
    }

    public String getAddress() {
        return address.get();
    }

    public SimpleStringProperty addressProperty() {
        return address;
    }

    public void setAddress(String address) {
        this.address.set(address);
    }


    public String getDate() {
        return date.get();
    }

    public SimpleStringProperty dateProperty() {
        return date;
    }

    public void setDate(String date) {
        this.date.set(date);
    }
}
