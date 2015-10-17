package kanjimatcher;

import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JTextField;

public class AssignmentController {

	private int index;
	private PairInfo<List<JTextField>> inputs;

	public AssignmentController() {
		reset(null);
	}

	public int size() {
		return inputs.FURIGANA.size();
	}

	public boolean hasNext() {
		if (index + 1 < inputs.FURIGANA.size()) {
			highlight(index);
		}
		return index < inputs.FURIGANA.size();
	}

	public JTextField next() {
		if (!hasNext()) {
			return null;
		}

		highlight(index + 1);
		return inputs.FURIGANA.get(index++);
	}

	public boolean hasPrev() {
		highlight(index);
		return index != 0;
	}

	public JTextField prev() {
		if (!hasPrev()) {
			return null;
		}
		index--;
		highlight(index);
		return inputs.FURIGANA.get(index);
	}

	private void highlight(int index) {
		if (inputs == null || index == inputs.KANJI.size()) {
			return;
		}

		inputs.KANJI.get(index).requestFocus();
		inputs.KANJI.get(index).selectAll();
	}

	public AssignmentController(PairInfo<List<JTextField>> inputs) {
		reset(inputs);
	}

	public void reset(PairInfo<List<JTextField>> inputs) {
		index = 0;
		this.inputs = inputs;
		highlight(index);
	}

	public PairInfo<List<String>> collect() {
		PairInfo<List<String>> results = null;
		if (this.inputs != null) {
			List<String> furigana = this.inputs.FURIGANA.stream().map(field -> field.getText()).collect(Collectors.toList());
			List<String> kanji = this.inputs.KANJI.stream().map(field -> field.getText()).collect(Collectors.toList());

			results = new PairInfo<List<String>>(kanji, furigana);
		}
		return results;
	}
}
