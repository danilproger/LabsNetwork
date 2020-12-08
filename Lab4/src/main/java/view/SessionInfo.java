package view;

import proto.SnakesProto;

import java.net.InetAddress;

public class SessionInfo {
	private InetAddress ip;
	private int port;

	private String name;

	private int width;
	private int height;
	private int baseFood;
	private double foodMultiplyer;
	private double foodDropChance;
	private int numOfPlayers;
	private boolean canJoin;

	private SnakesProto.GameConfig gameConfig;

	public SessionInfo(InetAddress ip, int port, String name,
					   int width, int height,
					   int baseFood, double foodMultilpyer, double foodDropChance,
					   int numOfPlayers, boolean canJoin, SnakesProto.GameConfig gameConfig) {

		this.ip = ip;
		this.port = port;
		this.name = name;
		this.width = width;
		this.height = height;
		this.baseFood = baseFood;
		this.foodMultiplyer = foodMultilpyer;
		this.foodDropChance = foodDropChance;
		this.numOfPlayers = numOfPlayers;
		this.canJoin = canJoin;
		this.gameConfig = gameConfig;
	}

	public InetAddress getIp() {
		return ip;
	}

	public void setIp(InetAddress ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}


	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getBaseFood() {
		return baseFood;
	}

	public void setBaseFood(int baseFood) {
		this.baseFood = baseFood;
	}

	public double getFoodMultiplyer() {
		return foodMultiplyer;
	}

	public void setFoodMultiplyer(double foodMultiplyer) {
		this.foodMultiplyer = foodMultiplyer;
	}

	public double getFoodDropChance() {
		return foodDropChance;
	}

	public void setFoodDropChance(double foodDropChance) {
		this.foodDropChance = foodDropChance;
	}

	public int getNumOfPlayers() {
		return numOfPlayers;
	}

	public void setNumOfPlayers(int numOfPlayers) {
		this.numOfPlayers = numOfPlayers;
	}

	public boolean isCanJoin() {
		return canJoin;
	}

	public void setCanJoin(boolean canJoin) {
		this.canJoin = canJoin;
	}

	public SnakesProto.GameConfig getGameConfig() {
		return gameConfig;
	}

	public void setGameConfig(SnakesProto.GameConfig gameConfig) {
		this.gameConfig = gameConfig;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}