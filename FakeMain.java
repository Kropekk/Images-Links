package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Stage;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FakeMain extends Application {
    private TableView<MyLink> table = new TableView<>();
    private Stage mainStage;
    private Text errorText = new Text("");
    private ArrayList<UrlResult> visitedLinks = new ArrayList<>();
    private Connection conn;
    private Statement stat;
    private final Popup popup = new Popup();

    @Override
    public void start(Stage primaryStage) throws Exception{
        initiateDB();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                conn.close();
                System.out.println("connection closed");
            }
            catch (Exception e) {e.printStackTrace();}
        }));
        mainStage = primaryStage;
        mainStage.setTitle("Links TCS");
        Label waitLabel = new Label("Wait");
        waitLabel.setStyle("-fx-background-color: red;");
        waitLabel.setFont(new Font(80));
        popup.getContent().add(waitLabel);
        mainStage.setScene(createFirstScene());
        mainStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

    private Scene createFirstScene() {
        errorText.setFont(new Font(20));
        errorText.setFill(Color.RED);
        VBox vbox = new VBox(15.);
        HBox hbox = new HBox();
        TextField textField = new TextField();
        Button searchButton = new Button("Go!");
        searchButton.setOnAction((e) -> {
            popup.show(mainStage);
            final UrlResult[] res = new UrlResult[1];
            ExecutorService executorService = Executors.newFixedThreadPool(1);
            executorService.execute(() -> {
                res[0] = new UrlResult(textField.getText()).handle(); // these computations are heavy
                Platform.runLater(() -> {
                    if (res[0] != null) {
                        processLink(res[0]);
                    }
                    else {
                        errorText.setText("Wrong URL!");
                    }
                    popup.hide();
                });
            });
            executorService.shutdown();
        });
        hbox.getChildren().addAll(textField, searchButton);
        hbox.setAlignment(Pos.CENTER);

        table.setEditable(true);
        TableColumn addressCol = new TableColumn("Vistied addresses");
        addressCol.setMinWidth(400);
        addressCol.setCellValueFactory(
                new PropertyValueFactory<MyLink, String>("address"));

        TableColumn dateCol = new TableColumn("Date (UTC)");
        dateCol.setMinWidth(200);
        dateCol.setCellValueFactory(
                new PropertyValueFactory<MyLink, String>("date"));

        table.setItems(FXCollections.observableArrayList(loadDataFromDB()));
        table.getColumns().addAll(addressCol, dateCol);

        table.setRowFactory(tv -> {
            TableRow<MyLink> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    table.getSelectionModel().clearSelection();
                    popup.show(mainStage);
                    final UrlResult[] res = new UrlResult[1];
                    ExecutorService executorService = Executors.newFixedThreadPool(1);
                    executorService.execute(() -> {
                        res[0] = new UrlResult(row.getItem().getAddress()).handle(); // these computations are heavy
                        Platform.runLater(() -> {
                            if (res[0] != null) {
                                processLink(res[0]);
                            }
                            else {
                                errorText.setText("Wrong URL!");
                            }
                            popup.hide();
                        });
                    });
                    executorService.shutdown();
                }
            });
            return row;
        });
        vbox.getChildren().addAll(hbox, errorText, table);
        return new Scene(vbox, 600, 600);
    }

    private Scene createSecondScene(UrlResult urlRes) {
        table.getColumns().clear();
        TableColumn addressCol = new TableColumn("Available addresses");
        addressCol.setMinWidth(600);
        addressCol.setCellValueFactory(
                new PropertyValueFactory<MyLink, String>("address"));
        table.setItems(FXCollections.observableArrayList(urlRes.getHyperlinks()));
        table.getColumns().addAll(addressCol);
        table.setEditable(false);


        VBox vbox = new VBox(15.);
        VBox topbox = new VBox(20.);
        Text imgInfo = new Text("Number of images: " + urlRes.getNumberOfImages() + "\nSum of their sizes:" + urlRes.getImagesSize());
        imgInfo.setFont(new Font(20));
        Text currentAddressText = new Text("current Address:\n" + urlRes.getAddress());
        currentAddressText.setFont(new Font(20));

        Button prevButton = new Button("Previous page");
        prevButton.setOnAction((e) -> {
            if (visitedLinks.size() <= 1) {
                errorText.setText("No previous page!");
            }
            else {
                errorText.setText("");
                visitedLinks.remove(visitedLinks.size()-1);
                UrlResult res = visitedLinks.get(visitedLinks.size()-1);
                mainStage.setScene(createSecondScene(res));
            }
        });
        topbox.getChildren().addAll(currentAddressText, imgInfo, errorText, prevButton);
        prevButton.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(topbox, table);
        return new Scene(vbox, 600, 600);
    }

    private void initiateDB () throws Exception {
        final String DRIVER = "org.sqlite.JDBC";
        final String DB_URL = "jdbc:sqlite:linki.db";
        Class.forName(DRIVER);
        conn = DriverManager.getConnection(DB_URL);
        stat = conn.createStatement();
        String createLinks = "CREATE TABLE IF NOT EXISTS links (id_link INTEGER PRIMARY KEY AUTOINCREMENT, address varchar(1023), visit_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
//        String createLinks = "CREATE TABLE IF NOT EXISTS links (id_link INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                "address varchar(1023), visit_date TIMESTAMP DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')))";
        stat.execute(createLinks);
    }

    private void addRecordToTable(String address) throws Exception {
        PreparedStatement prepStmt = conn.prepareStatement("insert into links (address) values (?);");
        prepStmt.setString(1, address);
        prepStmt.execute();
    }

    private List<MyLink> loadDataFromDB() {
        ArrayList<MyLink> lst = new ArrayList<>();
        try {
            ResultSet result = stat.executeQuery("SELECT address, visit_date FROM links ORDER BY id_link DESC");
            while (result.next()) {
                lst.add(new MyLink(result.getString("address"), result.getString("visit_date")));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("loadDataFromDBFailed");
        }
        return lst;
    }

    private synchronized void processLink(UrlResult res) {
            visitedLinks.add(res);
            mainStage.setScene(createSecondScene(res));
            errorText.setText("");
            try {
                addRecordToTable(res.getAddress());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
    }
}


