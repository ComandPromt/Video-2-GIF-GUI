package main;

import java.util.TimerTask;

public class TiempoVideo extends TimerTask {

	@Override
	public void run() {
		PlayerFrame.ponerTiempos();
	}
}
