package net.bigtangle.wallet.activity.settings.model;

public class ServerInfoItem {

    private String serverName;

    private String connectionURL;

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getConnectionURL() {
        return connectionURL;
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    public static ServerInfoItem build(String serverName, String connectionURL) {
        ServerInfoItem serverInfoItem = new ServerInfoItem();
        serverInfoItem.setServerName(serverName);
        serverInfoItem.setConnectionURL(connectionURL);
        return serverInfoItem;
    }
}
