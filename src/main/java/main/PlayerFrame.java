package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.elements.PlayBin;

import com.sun.jna.Platform;

import lib.SimpleVideoComponent;

public class PlayerFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private static JSlider positionSlider = new JSlider();

	private JPanel contentPane;

	private double segundosInicio;

	private double segundosFin;

	private PlayBin playbin;

	private final JFileChooser fileChooser = new JFileChooser();

	private JTextField inicio;

	private JTextField fin;

	private JTextField textField_2;

	private JTextField textField_3;

	private JTextField textField_4;

	private JTextField textField_5;

	private JTextField textField_6;

	private JTextField textField_7;

	private JTextField textField_8;

	private String video;

	private static JLabel duracion;

	private static JLabel tiempo;

	private static long segundosActual;

	private static long duration;

	private static double position = 0;

	final JLabel playPauseButton = new JLabel("");

	double convertirASegundos(String duracionVideo) {

		double horas = Double.parseDouble(duracionVideo.substring(0, duracionVideo.indexOf(":")));

		if (horas > 0) {
			horas *= 3600f;
		}

		double minutos = Double
				.parseDouble(duracionVideo.substring(duracionVideo.indexOf(":") + 1, duracionVideo.lastIndexOf(":")));

		if (minutos > 0) {
			minutos *= 60f;
		}

		double segundos = Double
				.parseDouble(duracionVideo.substring(duracionVideo.lastIndexOf(":") + 1, duracionVideo.length()));

		return horas + minutos + segundos;
	}

	private void play() {

		boolean playing = playbin.isPlaying();

		if (playing) {

			playbin.pause();
		}

		else {
			playbin.play();
		}

		ponerTiempos();

		playPauseButton
				.setIcon(new ImageIcon(getClass().getResource("/play-pause" + (playing ? "-active" : "") + ".png")));
	}

	private void abrirVideo() {

		int returnValue = fileChooser.showOpenDialog(contentPane);

		if (returnValue == JFileChooser.APPROVE_OPTION) {

			video = fileChooser.getSelectedFile().toURI().toString();

			video = video.replace("%20", " ");

			video = video.replace("file:", "");

			video = video.trim();

			playbin.stop();

			playbin.setURI(fileChooser.getSelectedFile().toURI());

			playbin.play();

		}

		verTiempos(false);

	}

	static LinkedList<Object> saberTiempo() {

		LinkedList<Object> tiempo = new LinkedList<Object>();

		position = positionSlider.getValue() / 1000.0;

		long segundos = duration / 1000000000;

		tiempo.add(position);

		tiempo.add(segundos);

		return tiempo;

	}

	private String calcularPosicionVideo() {
		double segundos = Double.parseDouble(
				tiempo.getText().substring(tiempo.getText().lastIndexOf(":") + 1, tiempo.getText().length()).trim());
		segundos += position;

		String positionVideo = tiempo.getText().substring(0, tiempo.getText().lastIndexOf(":") + 1) + segundos;
		return positionVideo;
	}

	static String calcularSegundosActual(Object obj1, Object obj2) {

		double inicio = (double) obj1;

		long segundos = (long) obj2;

		segundosActual = Math.round((inicio / segundos) * Math.pow(segundos, 2));

		return calcularTiempo(segundosActual);

	}

	public static String calcularTiempo(long segundos) {

		int minutos = 0;

		int horas = 0;

		if (segundos == 60) {
			minutos = 1;
			segundos = 0;
		}

		minutos = (int) (segundos / 60);

		int calculoSegundos = 0;

		calculoSegundos = 60 * minutos;

		segundos -= calculoSegundos;

		if (minutos == 60) {
			horas = 1;
			minutos = 0;
			segundos = 0;
		}

		if (minutos > 60) {

			if (minutos % 60 == 0) {
				horas = minutos / 60;
				minutos = 0;
				segundos = 0;
			}

			else {

				int contador = 0;

				int horaProxima = 120;

				int siguienteHora = 0;

				while (contador == 0) {

					if (minutos < horaProxima) {
						contador = horaProxima;
					}

					else {

						siguienteHora = horaProxima + 60;

						if (minutos > horaProxima && minutos < siguienteHora) {
							contador = siguienteHora;
						}

						horaProxima = siguienteHora;

					}
				}

				horas = minutos / 60;

				minutos = 60 - (horaProxima - minutos);

			}

		}

		String ceroHoras = "";
		String ceroMinutos = "";

		String ceroSegundos = "";

		if (horas <= 9) {
			ceroHoras = "0";
		}

		if (minutos <= 9) {
			ceroMinutos = "0";
		}

		if (segundos <= 9) {
			ceroSegundos = "0";
		}

		return ceroHoras + horas + " : " + ceroMinutos + minutos + " : " + ceroSegundos + segundos;

	}

	private void verTiempos(boolean adjust) {

		duration = playbin.queryDuration(TimeUnit.NANOSECONDS);

		System.out.println("duration: " + duration);

		if (duration < 0 && video != null) {

			try {

				String resultado;

				ProcessBuilder processBuilder = new ProcessBuilder("ffprobe", video);

				processBuilder.redirectErrorStream(true);

				Process process = processBuilder.start();
				StringBuilder processOutput = new StringBuilder();

				try (BufferedReader processOutputReader = new BufferedReader(
						new InputStreamReader(process.getInputStream()));) {

					String readLine;

					while ((readLine = processOutputReader.readLine()) != null) {

						if (readLine.contains("Duration")) {
							processOutput.append(readLine + System.lineSeparator());
						}

					}

				}

				resultado = processOutput.toString().trim();

				resultado = resultado.substring(resultado.indexOf("Duration") + 9, resultado.indexOf(","));

				resultado = resultado.trim();

				convertirANanoSegundos(resultado, true);

			}

			catch (Exception e) {
				e.printStackTrace();
			}

		}

		LinkedList<Object> cuenta = new LinkedList<Object>();

		cuenta = ponerTiempos();

		if (adjust)

		{

			cuenta.set(0, segundosInicio);

			if (segundosFin > 0) {

				duration = (long) segundosFin;
				System.out.println("duracion: " + duration);
			}

		}

		playbin.seek((long) ((double) cuenta.get(0) * duration), TimeUnit.NANOSECONDS);

		if (adjust) {
			play();
		}

	}

	private long convertirANanoSegundos(String duracionVideo, boolean filtro) {

		long nanosegundos;

		nanosegundos = (long) convertirASegundos(duracionVideo) * 1000000000;

		if (filtro) {

			duration = nanosegundos;

			tiempo.setText(calcularSegundosActual(0D, nanosegundos));

			duracion.setText("" + calcularTiempo((long) nanosegundos));

		}

		return nanosegundos;

	}

	static LinkedList<Object> ponerTiempos() {

		LinkedList<Object> cuenta;

		cuenta = saberTiempo();

		tiempo.setText(calcularSegundosActual(cuenta.get(0), cuenta.get(1)));

		duracion.setText("" + calcularTiempo((long) cuenta.get(1)));

		return cuenta;
	}

	private void verVideo(boolean adjust) {

		if (positionSlider.getValueIsAdjusting()) {

			verTiempos(adjust);

		}

	}

	public static void initialize(boolean windows) throws Exception {

		System.setProperty("awt.useSystemAAFontSettings", "lcd");

		System.setProperty("swing.aatext", "true");

		if (windows) {
			System.setProperty("gstreamer.GstNative.nameFormats", "%s-1.0-0|%s-1.0|%s-0|%s|lib%s|lib%s-0");
		}

		for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {

			if ("Nimbus".equals(info.getName())) {

				UIManager.setLookAndFeel(info.getClassName());

				break;

			}
		}

	}

	public static void main(String[] args) {

		positionSlider = new JSlider(0, 1000);

		positionSlider.setBorder(null);

		Gst.init();

		try {

			initialize(Platform.isWindows());

			PlayerFrame frame = new PlayerFrame();

			frame.setVisible(true);

			if (args.length > 0) {
				frame.openFile(args[0]);
			}

			frame.pack();

			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			// make the frame half the height and width
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int height = screenSize.height;
			int width = screenSize.width;
			frame.setSize(width / 2, height / 2);

			// center the jframe on screen
			frame.setLocationRelativeTo(null);

			frame.setVisible(true);

		}

		catch (Exception e) {
			e.printStackTrace();
		}

	}

	public PlayerFrame() {

		addKeyListener(new KeyAdapter() {

			@Override

			public void keyPressed(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play();
				}

			}

		});

		setTitle("Easy Video 2 GIF");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setSize(640, 530);

		JMenuBar menuBar = new JMenuBar();

		setJMenuBar(menuBar);

		JMenuItem mntmNewMenuItem = new JMenuItem("Abrir Video (CTRL+O)\n");

		mntmNewMenuItem.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				abrirVideo();
			}

		});

		menuBar.add(mntmNewMenuItem);

		JMenuItem mntmNewMenuItem_1 = new JMenuItem("Configuración");

		menuBar.add(mntmNewMenuItem_1);

		contentPane = new JPanel();
		contentPane.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play();
				}

			}

		});

		contentPane.setForeground(Color.WHITE);

		contentPane.setBackground(Color.DARK_GRAY);

		setContentPane(contentPane);

		final SpringLayout sl_contentPane = new SpringLayout();

		contentPane.setLayout(sl_contentPane);

		final String[] videoExts = new String[] { "asf", "avi", "3gp", "mp4", "mov", "flv", "mpg", "ts", "mkv", "webm",
				"mxf", "ogg" };

		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Video File", videoExts));

		JLabel openFileButton = new JLabel("");

		openFileButton.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {

				abrirVideo();

			}

		});

		openFileButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK), "open");

		openFileButton.getActionMap().put("open", new AbstractAction("open") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {

				int returnValue = fileChooser.showOpenDialog(contentPane);

				if (returnValue == JFileChooser.APPROVE_OPTION) {
					playbin.stop();
					playbin.setURI(fileChooser.getSelectedFile().toURI());
					playbin.play();
				}

			}

		});

		openFileButton.setToolTipText("");

		openFileButton.setIcon(new ImageIcon(getClass().getResource("/open-file.png")));

		sl_contentPane.putConstraint(SpringLayout.EAST, openFileButton, -4, SpringLayout.EAST, contentPane);

		contentPane.add(openFileButton);

		final JLabel playPauseButton = new JLabel("");

		playPauseButton.setFont(new Font("Dialog", Font.BOLD, 12));

		sl_contentPane.putConstraint(SpringLayout.NORTH, openFileButton, 0, SpringLayout.NORTH, playPauseButton);

		playPauseButton.addMouseListener(new MouseAdapter() {

			@Override

			public void mouseClicked(MouseEvent e) {

				play();
			}

		});

		playPauseButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
				"playpause");

		playPauseButton.getActionMap().put("playpause", new AbstractAction("playpause") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {

				play();
			}

		});

		playPauseButton.setVerticalAlignment(SwingConstants.BOTTOM);

		playPauseButton.setHorizontalAlignment(SwingConstants.CENTER);

		playPauseButton.setIcon(new ImageIcon(getClass().getResource("/play-pause.png")));

		playPauseButton.setToolTipText("");

		contentPane.add(playPauseButton);

		sl_contentPane.putConstraint(SpringLayout.WEST, positionSlider, 218, SpringLayout.WEST, contentPane);

		sl_contentPane.putConstraint(SpringLayout.EAST, positionSlider, -45, SpringLayout.EAST, contentPane);

		sl_contentPane.putConstraint(SpringLayout.WEST, openFileButton, 6, SpringLayout.EAST, positionSlider);

		sl_contentPane.putConstraint(SpringLayout.EAST, playPauseButton, -6, SpringLayout.WEST, positionSlider);

		positionSlider.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {

				verVideo(false);

			}

		});

		positionSlider.setValue(0);

		positionSlider.setBackground(Color.DARK_GRAY);

		contentPane.add(positionSlider);

		new Timer(50, new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				if (playbin == null || positionSlider == null)

					return;

				if (!positionSlider.getValueIsAdjusting() && playbin.isPlaying()) {

					long dur = playbin.queryDuration(TimeUnit.NANOSECONDS);

					long pos = playbin.queryPosition(TimeUnit.NANOSECONDS);

					if (dur > 0) {

						double relPos = (double) pos / dur;

						positionSlider.setValue((int) (relPos * 1000));

					}

					if (dur == pos && dur > 0) {

						playbin.seek(0);

						playbin.stop();
					}

				}

			}

		}).start();

		SimpleVideoComponent videoOutput = new SimpleVideoComponent();
		sl_contentPane.putConstraint(SpringLayout.EAST, openFileButton, 0, SpringLayout.EAST, videoOutput);
		sl_contentPane.putConstraint(SpringLayout.NORTH, positionSlider, 15, SpringLayout.SOUTH, videoOutput);
		sl_contentPane.putConstraint(SpringLayout.NORTH, playPauseButton, 13, SpringLayout.SOUTH, videoOutput);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, videoOutput, -269, SpringLayout.SOUTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.NORTH, videoOutput, 10, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.WEST, videoOutput, 184, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, videoOutput, -10, SpringLayout.EAST, contentPane);
		contentPane.add(videoOutput);

		inicio = new JTextField();
		inicio.setEditable(false);
		sl_contentPane.putConstraint(SpringLayout.WEST, inicio, 253, SpringLayout.WEST, contentPane);
		inicio.setHorizontalAlignment(SwingConstants.CENTER);
		contentPane.add(inicio);
		inicio.setColumns(10);

		JLabel playPauseButton_2 = new JLabel("");
		sl_contentPane.putConstraint(SpringLayout.NORTH, playPauseButton_2, 279, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.WEST, playPauseButton_2, 630, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, playPauseButton_2, -254, SpringLayout.SOUTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, playPauseButton_2, -43, SpringLayout.EAST, contentPane);
		playPauseButton_2.setVerticalAlignment(SwingConstants.BOTTOM);
		playPauseButton_2.setToolTipText("Play/Pause (SPACE)");
		playPauseButton_2.setHorizontalAlignment(SwingConstants.LEFT);
		contentPane.add(playPauseButton_2);

		JButton btnNewButton_1_1 = new JButton("<|");
		btnNewButton_1_1.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play();
				}
			}
		});

		btnNewButton_1_1.addMouseListener(new MouseAdapter() {
			@Override

			public void mousePressed(MouseEvent e) {

				String positionVideo = calcularPosicionVideo();

				segundosInicio = convertirASegundos(positionVideo);

				if (segundosFin > 0f && (segundosInicio > segundosFin)) {

					segundosInicio = 0;

					positionVideo = "00:00:0.0";

				}
				System.out.println(positionVideo + " - " + duration);
				inicio.setText(positionVideo);

			}

		});

		sl_contentPane.putConstraint(SpringLayout.EAST, inicio, -6, SpringLayout.WEST, btnNewButton_1_1);
		sl_contentPane.putConstraint(SpringLayout.NORTH, btnNewButton_1_1, 27, SpringLayout.SOUTH, positionSlider);
		sl_contentPane.putConstraint(SpringLayout.NORTH, inicio, 6, SpringLayout.NORTH, btnNewButton_1_1);
		btnNewButton_1_1.setHorizontalAlignment(SwingConstants.LEFT);
		btnNewButton_1_1.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(btnNewButton_1_1);

		JLabel lblNewLabel = new JLabel("Inicio");
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblNewLabel, -1, SpringLayout.NORTH, inicio);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblNewLabel, 0, SpringLayout.WEST, playPauseButton);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblNewLabel, 0, SpringLayout.SOUTH, inicio);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblNewLabel, -6, SpringLayout.WEST, inicio);
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setForeground(Color.WHITE);
		lblNewLabel.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(lblNewLabel);

		fin = new JTextField();
		fin.setEditable(false);
		sl_contentPane.putConstraint(SpringLayout.NORTH, fin, 33, SpringLayout.SOUTH, positionSlider);
		sl_contentPane.putConstraint(SpringLayout.WEST, fin, 470, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, fin, -86, SpringLayout.EAST, contentPane);
		fin.setHorizontalAlignment(SwingConstants.CENTER);
		fin.setColumns(10);
		contentPane.add(fin);

		JButton btnNewButton_1_1_1 = new JButton("<|");

		btnNewButton_1_1_1.addKeyListener(new KeyAdapter() {

			@Override

			public void keyPressed(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play();
				}

			}

		});

		btnNewButton_1_1_1.addMouseListener(new MouseAdapter() {

			@Override

			public void mousePressed(MouseEvent e) {

				String positionVideo = calcularPosicionVideo();

				segundosFin = convertirASegundos(positionVideo);

				if (segundosFin > 0f && (segundosInicio > segundosFin)) {

					segundosFin = convertirASegundos(duracion.getText());

					positionVideo = duracion.getText();

				}

				fin.setText(positionVideo);

			}

		});

		sl_contentPane.putConstraint(SpringLayout.NORTH, btnNewButton_1_1_1, -6, SpringLayout.NORTH, inicio);
		sl_contentPane.putConstraint(SpringLayout.WEST, btnNewButton_1_1_1, 6, SpringLayout.EAST, fin);
		btnNewButton_1_1_1.setHorizontalAlignment(SwingConstants.LEFT);
		btnNewButton_1_1_1.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(btnNewButton_1_1_1);

		JLabel lblDuracin = new JLabel("Duración");
		sl_contentPane.putConstraint(SpringLayout.WEST, lblDuracin, 10, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblDuracin, -5, SpringLayout.WEST, videoOutput);
		lblDuracin.setHorizontalAlignment(SwingConstants.CENTER);
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblDuracin, 10, SpringLayout.NORTH, contentPane);
		lblDuracin.setFont(new Font("Dialog", Font.BOLD, 16));
		lblDuracin.setForeground(Color.WHITE);
		contentPane.add(lblDuracin);

		textField_2 = new JTextField();
		textField_2.setEditable(false);
		sl_contentPane.putConstraint(SpringLayout.NORTH, textField_2, 6, SpringLayout.SOUTH, lblDuracin);
		sl_contentPane.putConstraint(SpringLayout.EAST, textField_2, -33, SpringLayout.WEST, videoOutput);
		contentPane.add(textField_2);
		textField_2.setColumns(10);

		JLabel lblDuracin_1 = new JLabel("Ancho");
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblDuracin_1, 13, SpringLayout.SOUTH, textField_2);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblDuracin_1, 10, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblDuracin_1, -5, SpringLayout.WEST, videoOutput);
		lblDuracin_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblDuracin_1.setForeground(Color.WHITE);
		lblDuracin_1.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(lblDuracin_1);

		textField_3 = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.NORTH, textField_3, 6, SpringLayout.SOUTH, lblDuracin_1);
		sl_contentPane.putConstraint(SpringLayout.WEST, textField_3, 0, SpringLayout.WEST, textField_2);
		sl_contentPane.putConstraint(SpringLayout.EAST, textField_3, 0, SpringLayout.EAST, textField_2);
		textField_3.setHorizontalAlignment(SwingConstants.CENTER);
		textField_3.setColumns(10);
		contentPane.add(textField_3);

		JLabel lblDuracin_1_1 = new JLabel("Alto");
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblDuracin_1_1, 15, SpringLayout.SOUTH, textField_3);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblDuracin_1_1, 10, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblDuracin_1_1, -5, SpringLayout.WEST, videoOutput);
		lblDuracin_1_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblDuracin_1_1.setForeground(Color.WHITE);
		lblDuracin_1_1.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(lblDuracin_1_1);

		textField_4 = new JTextField();
		textField_4.setHorizontalAlignment(SwingConstants.CENTER);
		sl_contentPane.putConstraint(SpringLayout.NORTH, textField_4, 6, SpringLayout.SOUTH, lblDuracin_1_1);
		sl_contentPane.putConstraint(SpringLayout.WEST, textField_4, 0, SpringLayout.WEST, textField_2);
		textField_4.setColumns(10);
		contentPane.add(textField_4);

		JLabel lblDuracin_1_1_1 = new JLabel("Framerate");
		sl_contentPane.putConstraint(SpringLayout.WEST, lblDuracin_1_1_1, 0, SpringLayout.WEST, lblDuracin);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblDuracin_1_1_1, -5, SpringLayout.WEST, videoOutput);
		lblDuracin_1_1_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblDuracin_1_1_1.setForeground(Color.WHITE);
		lblDuracin_1_1_1.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(lblDuracin_1_1_1);

		textField_5 = new JTextField();
		textField_5.setHorizontalAlignment(SwingConstants.CENTER);
		sl_contentPane.putConstraint(SpringLayout.NORTH, textField_5, 226, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblDuracin_1_1_1, -6, SpringLayout.NORTH, textField_5);
		sl_contentPane.putConstraint(SpringLayout.WEST, textField_5, 0, SpringLayout.WEST, textField_2);
		textField_5.setColumns(10);
		contentPane.add(textField_5);

		JLabel lblFin = new JLabel("Fin");
		sl_contentPane.putConstraint(SpringLayout.EAST, btnNewButton_1_1, -6, SpringLayout.WEST, lblFin);
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblFin, -1, SpringLayout.NORTH, inicio);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblFin, 391, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblFin, -6, SpringLayout.WEST, fin);
		lblFin.setHorizontalAlignment(SwingConstants.CENTER);
		lblFin.setForeground(Color.WHITE);
		lblFin.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(lblFin);

		JLabel lblDuracin_1_1_1_1 = new JLabel("Calidad");
		sl_contentPane.putConstraint(SpringLayout.WEST, lblDuracin_1_1_1_1, 0, SpringLayout.WEST, lblDuracin);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblDuracin_1_1_1_1, -193, SpringLayout.SOUTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblDuracin_1_1_1_1, 14, SpringLayout.EAST, textField_2);
		lblDuracin_1_1_1_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblDuracin_1_1_1_1.setForeground(Color.WHITE);
		lblDuracin_1_1_1_1.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(lblDuracin_1_1_1_1);

		textField_6 = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.EAST, textField_6, 0, SpringLayout.EAST, btnNewButton_1_1_1);
		textField_6.setColumns(10);
		contentPane.add(textField_6);

		JLabel lblDuracin_1_1_1_1_1 = new JLabel("Desenfoque");
		sl_contentPane.putConstraint(SpringLayout.WEST, textField_6, 0, SpringLayout.WEST, lblDuracin_1_1_1_1_1);
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblDuracin_1_1_1_1_1, 22, SpringLayout.SOUTH,
				btnNewButton_1_1_1);
		sl_contentPane.putConstraint(SpringLayout.NORTH, textField_6, 6, SpringLayout.SOUTH, lblDuracin_1_1_1_1_1);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblDuracin_1_1_1_1_1, 488, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblDuracin_1_1_1_1_1, 0, SpringLayout.EAST, btnNewButton_1_1_1);
		lblDuracin_1_1_1_1_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblDuracin_1_1_1_1_1.setForeground(Color.WHITE);
		lblDuracin_1_1_1_1_1.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(lblDuracin_1_1_1_1_1);

		JLabel lblDuracin_1_1_1_1_1_1 = new JLabel("Nº Fotogramas");
		sl_contentPane.putConstraint(SpringLayout.WEST, lblDuracin_1_1_1_1_1_1, 0, SpringLayout.WEST, textField_2);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblDuracin_1_1_1_1_1_1, -433, SpringLayout.EAST,
				btnNewButton_1_1_1);
		lblDuracin_1_1_1_1_1_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblDuracin_1_1_1_1_1_1.setForeground(Color.WHITE);
		lblDuracin_1_1_1_1_1_1.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(lblDuracin_1_1_1_1_1_1);

		textField_7 = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblDuracin_1_1_1_1_1_1, -6, SpringLayout.NORTH, textField_7);
		sl_contentPane.putConstraint(SpringLayout.NORTH, textField_7, 358, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, textField_7, 0, SpringLayout.EAST, textField_2);
		sl_contentPane.putConstraint(SpringLayout.WEST, textField_7, 34, SpringLayout.WEST, contentPane);
		textField_7.setColumns(10);
		contentPane.add(textField_7);

		JCheckBox chckbxNewCheckBox = new JCheckBox("Optimizar");
		chckbxNewCheckBox.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play();
				}
			}
		});
		chckbxNewCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		chckbxNewCheckBox.setFont(new Font("Dialog", Font.BOLD, 16));
		chckbxNewCheckBox.setBackground(Color.DARK_GRAY);
		chckbxNewCheckBox.setForeground(Color.WHITE);
		contentPane.add(chckbxNewCheckBox);

		JComboBox comboBox = new JComboBox();
		comboBox.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play();
				}
			}
		});
		sl_contentPane.putConstraint(SpringLayout.NORTH, comboBox, 3, SpringLayout.SOUTH, lblDuracin_1_1_1_1);
		sl_contentPane.putConstraint(SpringLayout.WEST, comboBox, 0, SpringLayout.WEST, textField_2);
		sl_contentPane.putConstraint(SpringLayout.EAST, comboBox, -102, SpringLayout.WEST, inicio);
		contentPane.add(comboBox);

		JButton btnNewButton = new JButton("Previsualizar");
		btnNewButton.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play();
				}
			}
		});

		btnNewButton.addMouseListener(new MouseAdapter() {

			@Override

			public void mousePressed(MouseEvent e) {

				try {

					if (!inicio.getText().isEmpty() && !fin.getText().isEmpty() && segundosInicio < segundosFin) {

						verVideo(true);

					}

				}

				catch (Exception e1) {

					e1.printStackTrace();

				}

			}

		});

		sl_contentPane.putConstraint(SpringLayout.NORTH, btnNewButton, -4, SpringLayout.NORTH, textField_6);
		sl_contentPane.putConstraint(SpringLayout.WEST, btnNewButton, 117, SpringLayout.EAST, lblDuracin_1_1_1_1_1_1);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, btnNewButton, 0, SpringLayout.SOUTH, textField_7);
		sl_contentPane.putConstraint(SpringLayout.EAST, btnNewButton, -53, SpringLayout.WEST, textField_6);
		btnNewButton.setFont(new Font("Dialog", Font.BOLD, 14));
		contentPane.add(btnNewButton);

		JButton btnConvertir = new JButton("Convertir");

		sl_contentPane.putConstraint(SpringLayout.NORTH, chckbxNewCheckBox, 13, SpringLayout.NORTH, btnConvertir);
		sl_contentPane.putConstraint(SpringLayout.EAST, chckbxNewCheckBox, -38, SpringLayout.WEST, btnConvertir);
		sl_contentPane.putConstraint(SpringLayout.WEST, btnConvertir, 14, SpringLayout.WEST, fin);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, btnConvertir, -26, SpringLayout.SOUTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, btnConvertir, -27, SpringLayout.EAST, contentPane);
		btnConvertir.setFont(new Font("Dialog", Font.BOLD, 14));
		contentPane.add(btnConvertir);

		JCheckBox chckbxNewCheckBox_1 = new JCheckBox("Limitación de tamaño");
		chckbxNewCheckBox_1.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play();
				}
			}
		});
		sl_contentPane.putConstraint(SpringLayout.EAST, chckbxNewCheckBox_1, -393, SpringLayout.EAST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.WEST, chckbxNewCheckBox, 50, SpringLayout.EAST, chckbxNewCheckBox_1);
		sl_contentPane.putConstraint(SpringLayout.NORTH, btnConvertir, 0, SpringLayout.NORTH, chckbxNewCheckBox_1);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, chckbxNewCheckBox_1, -54, SpringLayout.SOUTH, contentPane);
		chckbxNewCheckBox_1.setBackground(Color.DARK_GRAY);
		chckbxNewCheckBox_1.setFont(new Font("Dialog", Font.BOLD, 16));
		chckbxNewCheckBox_1.setForeground(Color.WHITE);
		contentPane.add(chckbxNewCheckBox_1);

		JComboBox comboBox_1 = new JComboBox();
		comboBox_1.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play();
				}
			}
		});
		sl_contentPane.putConstraint(SpringLayout.SOUTH, comboBox_1, 0, SpringLayout.SOUTH, btnConvertir);
		sl_contentPane.putConstraint(SpringLayout.EAST, comboBox_1, -64, SpringLayout.WEST, chckbxNewCheckBox);
		contentPane.add(comboBox_1);

		textField_8 = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.WEST, comboBox_1, 14, SpringLayout.EAST, textField_8);
		sl_contentPane.putConstraint(SpringLayout.NORTH, textField_8, 6, SpringLayout.SOUTH, chckbxNewCheckBox_1);
		sl_contentPane.putConstraint(SpringLayout.EAST, textField_8, 0, SpringLayout.EAST, textField_2);
		textField_8.setHorizontalAlignment(SwingConstants.CENTER);
		textField_8.setColumns(10);
		contentPane.add(textField_8);

		tiempo = new JLabel("");
		sl_contentPane.putConstraint(SpringLayout.NORTH, tiempo, 6, SpringLayout.SOUTH, positionSlider);
		sl_contentPane.putConstraint(SpringLayout.WEST, tiempo, 215, SpringLayout.WEST, contentPane);
		tiempo.setForeground(Color.WHITE);
		contentPane.add(tiempo);

		duracion = new JLabel("");
		sl_contentPane.putConstraint(SpringLayout.NORTH, duracion, 6, SpringLayout.SOUTH, openFileButton);
		sl_contentPane.putConstraint(SpringLayout.EAST, duracion, 0, SpringLayout.EAST, playPauseButton_2);
		duracion.setForeground(Color.WHITE);
		contentPane.add(duracion);

		playbin = new PlayBin("GstDumbPlayer");

		playbin.setVideoSink(videoOutput.getElement());

	}

	public void openFile(String file) {

		File f = new File(file);

		playbin.stop();

		playbin.setURI(f.toURI());

		playbin.play();

	}
}
