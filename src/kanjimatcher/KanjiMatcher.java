package kanjimatcher;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import util.NewsEasyFormatUtil;

public class KanjiMatcher {

	private static final int WIDTH = 800, HEIGHT = 800;
	private static final int MAIN_GRID_ROWS = 2, MAIN_GRID_COLS = 1;
	private static final int KANJI_GRID_ROWS = 0, KANJI_GRID_COLS = 8;
	private static final String TITLE = "Kanji Matcher";

	private PairReader readings;
	private AssignmentController assigner;
	private JFrame frame;

	private static final File inputJSONFile = new File("res/output.json");
	private JSONObject inputRoot;
	private static final File outputJSONFile = new File("res/kanjiInfo.json");
	private static final File outputJSONTemplate = new File("res/kanjiInfoTemplate.json");
	private JSONObject outputRoot;
	
	private int currCount = 0;
	private int countTotal = 0;

	public static void main(String[] args) {
		new KanjiMatcher();
	}

	public KanjiMatcher() {
		if(!outputJSONFile.exists()){
			JSONObject template = NewsEasyFormatUtil.createJSONFromFile(outputJSONTemplate);
			NewsEasyFormatUtil.printJSONObject(outputJSONTemplate, template);
		}
		initFrame();
		assigner = new AssignmentController();
		SwingUtilities.invokeLater(() -> {
			getNextPair(null);
		});
		
	}

	private DatabaseItemWrapper getNextPair(PairInfo<List<String>> results) {
		if (results != null) {
			saveResult(readings.getCurr().READING_PAIR, results);
		}
		frame.getContentPane().repaint();
		if (readings == null) {
			Set<PairInfo<String>> preprocessedKeys = null;
			try (FileInputStream fis = new FileInputStream(outputJSONFile)) {
				JSONTokener tokener = new JSONTokener(fis);
				outputRoot = new JSONObject(tokener);
				preprocessedKeys = PairReader.readPreprocessedKeys(outputRoot);
				currCount = preprocessedKeys.size();
			} catch (JSONException | IOException e) {
				System.err.print("Failed to create word list from " + inputJSONFile.getAbsolutePath());
				e.printStackTrace();
			}
			try (FileInputStream fis = new FileInputStream(inputJSONFile)) {
				JSONTokener tokener = new JSONTokener(fis);
				inputRoot = new JSONObject(tokener);
				this.readings = new PairReader(inputRoot, preprocessedKeys);
				this.countTotal = this.readings.size();
			} catch (JSONException | IOException e) {
				System.err.print("Failed to create word list from " + inputJSONFile.getAbsolutePath());
				e.printStackTrace();
			}
		}
		
		System.out.format("%s out of %s\n", currCount++, countTotal);

		if (readings.hasNext()) {
			DatabaseItemWrapper nextPair = readings.next();
			initGui(nextPair);
			return nextPair;
		} else {
			return null;
		}
	}

	private void UpdateKanjiFromText(String selectedText) {
		if (assigner.hasNext()) {
			JTextField next = assigner.next();
			// the 2* comes from having to add both kanji/reading for each element
			updateText(next, selectedText, MAIN_GRID_ROWS * (1 + ((2 * assigner.size()) / KANJI_GRID_COLS)), MAIN_GRID_COLS * KANJI_GRID_COLS);
		}
	}

	private void initFrame() {
		frame = new JFrame(TITLE);
		frame.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setResizable(false);
		frame.setVisible(true);
		frame.pack();
		frame.setLocationRelativeTo(null);
		
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent evt){
				int choice = JOptionPane.showConfirmDialog(null, "Do you want to save before closing?", "Save?", JOptionPane.YES_NO_OPTION);
				if(choice == JOptionPane.YES_OPTION){
					NewsEasyFormatUtil.printJSONObject(outputJSONFile, outputRoot);
				}
				System.exit(choice);
			}
		});
	}

	private void initGui(DatabaseItemWrapper databaseItem) {
		frame.setContentPane(new JPanel(new GridLayout(MAIN_GRID_ROWS, MAIN_GRID_COLS)));

		frame.getContentPane().add(addFuriganaText(databaseItem));

		frame.getContentPane().add(addKanjiFlow(databaseItem.READING_PAIR.KANJI));

		frame.setVisible(true);
	}

	private JPanel addKanjiFlow(String kanji) {
		JPanel kanjiFlow = new JPanel(new GridLayout(KANJI_GRID_ROWS, KANJI_GRID_COLS));

		List<JTextField> kanjiTextList = new ArrayList<>();
		List<JTextField> kanjiInputList = new ArrayList<>();

		for (int i = 0; i < kanji.length(); i++) {
			String k = kanji.substring(i, i + 1);

			JTextField kanjiText = new JTextField(k);
			kanjiText.setEditable(false);
			kanjiText.setHorizontalAlignment(JTextField.CENTER);

			// the 2* comes from having to add both kanji/reading for each element
			updateText(kanjiText, k, MAIN_GRID_ROWS * (1 + ((2 * kanji.length()) / KANJI_GRID_COLS)), MAIN_GRID_COLS * KANJI_GRID_COLS);

			JTextField kanjiInput = new JTextField();
			kanjiInput.setEditable(false);

			kanjiTextList.add(kanjiText);
			kanjiInputList.add(kanjiInput);

			kanjiFlow.add(kanjiText);
			kanjiFlow.add(kanjiInput);
		}

		PairInfo<List<JTextField>> inputs = new PairInfo<>(kanjiTextList, kanjiInputList);
		assigner.reset(inputs);

		return kanjiFlow;
	}

	private void submitClick() {
		PairInfo<List<String>> results = assigner.collect();
		for (String s : results.FURIGANA) {
			if (s.isEmpty()) {
				JOptionPane.showMessageDialog(frame, "One of the kanji was left blank. Result not submitted", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		getNextPair(results);
	}

	private void saveResult(PairInfo<String> input, PairInfo<List<String>> results) {
		SwingWorker<Void, Void> saver = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {

				JSONObject words = outputRoot.getJSONObject("words");
				if (!words.has(input.KANJI)) {
					words.put(input.KANJI, new JSONObject());
				}
				JSONObject wordInner = words.getJSONObject(input.KANJI);
				if (!wordInner.has(input.FURIGANA)) {
					wordInner.put(input.FURIGANA, new JSONArray());
				}

				JSONArray readingInner = wordInner.getJSONArray(input.FURIGANA);

				for (int i = 0; i < results.KANJI.size(); i++) {
					String furigana = results.FURIGANA.get(i);

					readingInner.put(furigana);

				}
				return null;
			}
		};
		saver.execute();
	}

	private JPanel addButtonControls(){
		JPanel controls = new JPanel(new GridLayout(2, 2));

		JButton submitButton = new JButton("Submit");
		submitButton.addActionListener(e -> submitClick());
		controls.add(submitButton);
		
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(e -> NewsEasyFormatUtil.printJSONObject(outputJSONFile, outputRoot));
		controls.add(saveButton);

		JButton backButton = new JButton("Back one kanji");
		backButton.addActionListener(e -> {
			if (assigner.hasPrev()) {
				assigner.prev();
			}
		});
		controls.add(backButton);

		JButton skipButton = new JButton("Skip one kanji");
		skipButton.addActionListener(e -> {
			if (assigner.hasNext()) {
				assigner.next();
			}
		});
		controls.add(skipButton);
		
		return controls;
	}
	private JPanel addFuriganaText(DatabaseItemWrapper databaseItem) {
		final int FURIGANA_PANEL_ROWS = 3, FURIGANA_PANEL_COLS = 1;
		JPanel furiganaPanel = new JPanel(new GridLayout(FURIGANA_PANEL_ROWS, FURIGANA_PANEL_COLS));
		
		furiganaPanel.add(addButtonControls());

		JTextField rawText = new JTextField();
		rawText.setEditable(false);
		rawText.setHorizontalAlignment(JTextField.CENTER);
		updateText(rawText, databaseItem.KEY , MAIN_GRID_ROWS * FURIGANA_PANEL_ROWS, MAIN_GRID_COLS * FURIGANA_PANEL_COLS);
		furiganaPanel.add(rawText);
		
		JTextField furiganaText = new JTextField();
		furiganaText.setEditable(false);
		furiganaText.setHorizontalAlignment(JTextField.CENTER);
		furiganaText.addActionListener(e -> UpdateKanjiFromText(furiganaText.getSelectedText()));
		updateText(furiganaText, databaseItem.READING_PAIR.FURIGANA , MAIN_GRID_ROWS * FURIGANA_PANEL_ROWS, MAIN_GRID_COLS * FURIGANA_PANEL_COLS);
		furiganaPanel.add(furiganaText);
		
		return furiganaPanel;
	}

	private static void updateText(JTextField text, String content, int rows, int columns) {
		text.setText(content);

		Font font = text.getFont();
		int textWidth = text.getFontMetrics(font).stringWidth(text.getText()) + 6; // the 6 is a magic number to add a margin
		double widthRatio = ((double) (WIDTH)) / (columns * textWidth);
		int maxSizeWidth = (int) (font.getSize() * widthRatio);
		int maxSizeHeight = (HEIGHT / rows) - 20; // the 20 is a magic number to add a margin
		int fontSize = maxSizeHeight > maxSizeWidth ? maxSizeWidth : maxSizeHeight;

		final int maxSize = 150;
		fontSize = fontSize > maxSize ? maxSize : fontSize;

		text.setFont(new Font(text.getFont().getName(), Font.PLAIN, fontSize));
	}
}
