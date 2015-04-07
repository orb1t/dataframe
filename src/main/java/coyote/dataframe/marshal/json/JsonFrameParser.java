package coyote.dataframe.marshal.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import coyote.dataframe.DataField;
import coyote.dataframe.DataFrame;

/**
 * Parse JSON text into DataFrames.
 */
public class JsonFrameParser {

	private static final int MIN_BUFFER_SIZE = 10;
	private static final int DEFAULT_BUFFER_SIZE = 1024;

	private final Reader reader;
	private final char[] buffer;
	private int bufferOffset;

	private int fill;
	private int line;
	private int lineOffset;

	// where we are in the current buffer
	private int index;

	// The current character under consideration
	private int current;

	// What has been captured so far
	private StringBuilder captureBuffer;

	// index into the current buffer where we start capturing our value
	private int captureStart;




	public JsonFrameParser(final String string) {
		this(new StringReader(string), Math.max(MIN_BUFFER_SIZE, Math.min(DEFAULT_BUFFER_SIZE, string.length())));
	}




	public JsonFrameParser(final Reader reader) {
		this(reader, DEFAULT_BUFFER_SIZE);
	}




	public JsonFrameParser(final Reader reader, final int buffersize) {
		this.reader = reader;
		buffer = new char[buffersize];
		line = 1;
		captureStart = -1;
	}




	/**
	 * Parse the data and return a list of DataFrames containing the data.
	 * 
	 * <p>Normally, there will only be one root value, but some applications may 
	 * have data which represents multiple arrays or objects. This method will 
	 * continue parsing until all objects (or arrays) are consumed.</p>
	 * 
	 * @return the data represented by the currently set string as one or more 
	 * DataFrames
	 */
	public List<DataFrame> parse() throws IOException {
		final List<DataFrame> retval = new ArrayList<DataFrame>();
		read();
		skipWhiteSpace();

		while ((current == '{') || (current == '[')) {
			retval.add(readRootValue());
			skipWhiteSpace();
		}

		if (!isEndOfText()) {
			throw error("Unexpected character");
		}
		return retval;

	}




	private DataFrame readRootValue() throws IOException {
		switch (current) {
		case 'n':
			return new DataFrame(new DataField(readNull()));
		case 't':
			return new DataFrame(new DataField(readTrue()));
		case 'f':
			return new DataFrame(new DataField(readFalse()));
		case '"':
			return new DataFrame(new DataField(readString()));
		case '[':
			return readArray();
		case '{':
			return readObject();
		case '-':
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
			return new DataFrame(new DataField(readNumber()));
		default:
			throw expected("value");
		}
	}




	private DataField readFieldValue(final String name) throws IOException {
		switch (current) {
		case 'n':
			return readNull();
		case 't':
			return readTrue();
		case 'f':
			return readFalse();
		case '"':
			return new DataField(name, readString());
		case '[':
			return new DataField(name, readArray());
		case '{':
			return new DataField(name, readObject());
		case ']':
		case ',':
			return new DataField(name, null);
		case '-':
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
			return new DataField(name, readNumber());
		default:
			throw expected("value");
		}
	}




	private DataFrame readArray() throws IOException {
		read();
		final DataFrame array = new DataFrame();
		skipWhiteSpace();
		if (readChar(']')) {
			return array;
		}
		do {
			skipWhiteSpace();
			final DataField field = readFieldValue(null);
			array.add(field);
			skipWhiteSpace();
		} while (readChar(','));
		if (!readChar(']')) {
			throw expected("',' or ']'");
		}
		return array;
	}




	/**
	 * Read the JSON object into a dataFrame
	 * @return
	 * @throws IOException
	 */
	private DataFrame readObject() throws IOException {
		read();
		final DataFrame object = new DataFrame();
		skipWhiteSpace();
		if (readChar('}')) {
			return object; // return an empty frame
		}

		do {
			// try to read the name
			skipWhiteSpace();
			final String name = readName();
			skipWhiteSpace();
			if (!readChar(':')) {
				throw expected("':'");
			}
			// next, read the value for this named field
			skipWhiteSpace();
			final DataField value = readFieldValue(name);
			object.add(value);
			skipWhiteSpace();
		} while (readChar(','));
		if (!readChar('}')) {
			throw expected("',' or '}'");
		}
		return object;
	}




	/**
	 * Read a quoted string.
	 * @return
	 * @throws IOException if not quoted
	 */
	private String readName() throws IOException {
		if (current != '"') {
			throw expected("name");
		}
		return readStringInternal();
	}




	private DataField readNull() throws IOException {
		read();
		readRequiredChar('u');
		readRequiredChar('l');
		readRequiredChar('l');
		return new DataField(null, null);
	}




	private DataField readTrue() throws IOException {
		read();
		readRequiredChar('r');
		readRequiredChar('u');
		readRequiredChar('e');
		return new DataField(null, true);
	}




	private DataField readFalse() throws IOException {
		read();
		readRequiredChar('a');
		readRequiredChar('l');
		readRequiredChar('s');
		readRequiredChar('e');
		return new DataField(null, false);
	}




	private void readRequiredChar(final char ch) throws IOException {
		if (!readChar(ch)) {
			throw expected("'" + ch + "'");
		}
	}




	private String readString() throws IOException {
		return readStringInternal();
	}




	private String readStringInternal() throws IOException {
		read();
		startCapture();
		while (current != '"') {
			if (current == '\\') {
				pauseCapture();
				readEscape();
				startCapture();
			} else if (current < 0x20) {
				throw expected("valid string character");
			} else {
				read();
			}
		}
		final String string = endCapture();
		read();
		return string;
	}




	private void readEscape() throws IOException {
		read();
		switch (current) {
		case '"':
		case '/':
		case '\\':
			captureBuffer.append((char) current);
			break;
		case 'b':
			captureBuffer.append('\b');
			break;
		case 'f':
			captureBuffer.append('\f');
			break;
		case 'n':
			captureBuffer.append('\n');
			break;
		case 'r':
			captureBuffer.append('\r');
			break;
		case 't':
			captureBuffer.append('\t');
			break;
		case 'u':
			final char[] hexChars = new char[4];
			for (int i = 0; i < 4; i++) {
				read();
				if (!isHexDigit()) {
					throw expected("hexadecimal digit");
				}
				hexChars[i] = (char) current;
			}
			captureBuffer.append((char) Integer.parseInt(new String(hexChars), 16));
			break;
		default:
			throw expected("valid escape sequence");
		}
		read();
	}




	private Object readNumber() throws IOException {
		startCapture();
		readChar('-');
		final int firstDigit = current;
		if (!readDigit()) {
			throw expected("digit");
		}
		if (firstDigit != '0') {
			while (readDigit()) {
			}
		}
		readFraction();
		readExponent();
		final String value = endCapture();
		// TODO: support more types like exponents and fractions
		try {
			return Long.parseLong(value);
		} catch (final NumberFormatException e) {
			// Ignore...just return the string if all else fails
		}
		return value; // for now, just return it as a string
	}




	private boolean readFraction() throws IOException {
		if (!readChar('.')) {
			return false;
		}
		if (!readDigit()) {
			throw expected("digit");
		}
		while (readDigit()) {
		}
		return true;
	}




	private boolean readExponent() throws IOException {
		if (!readChar('e') && !readChar('E')) {
			return false;
		}
		if (!readChar('+')) {
			readChar('-');
		}
		if (!readDigit()) {
			throw expected("digit");
		}
		while (readDigit()) {
		}
		return true;
	}




	private boolean readChar(final char ch) throws IOException {
		if (current != ch) {
			return false;
		}
		read();
		return true;
	}




	private boolean readDigit() throws IOException {
		if (!isDigit()) {
			return false;
		}
		read();
		return true;
	}




	/**
	 * Consume all whitespace.
	 * 
	 * @throws IOException
	 */
	private void skipWhiteSpace() throws IOException {
		while (isWhiteSpace()) {
			read();
		}
	}




	private void read() throws IOException {
		if (index == fill) {
			if (captureStart != -1) {
				captureBuffer.append(buffer, captureStart, fill - captureStart);
				captureStart = 0;
			}
			bufferOffset += fill;
			fill = reader.read(buffer, 0, buffer.length);
			index = 0;
			if (fill == -1) {
				current = -1;
				return;
			}
		}
		if (current == '\n') {
			line++;
			lineOffset = bufferOffset + index;
		}
		current = buffer[index++];
	}




	private void startCapture() {
		if (captureBuffer == null) {
			captureBuffer = new StringBuilder();
		}
		captureStart = index - 1;
	}




	private void pauseCapture() {
		final int end = current == -1 ? index : index - 1;
		captureBuffer.append(buffer, captureStart, end - captureStart);
		captureStart = -1;
	}




	/**
	 * Stop capturing the string value from the buffer and return what has been 
	 * captured so far.
	 * 
	 * <p>Essentially, return the string from the point we started capturing to
	 * the character prior to the one we just read in.</p>
	 * 
	 * @return the string in the buffer from the point of capture to now.
	 */
	private String endCapture() {
		final int end = current == -1 ? index : index - 1;
		String captured;
		if (captureBuffer.length() > 0) {
			captureBuffer.append(buffer, captureStart, end - captureStart);
			captured = captureBuffer.toString();
			captureBuffer.setLength(0);
		} else {
			captured = new String(buffer, captureStart, end - captureStart);
		}
		captureStart = -1;
		return captured;
	}




	private ParseException expected(final String expected) {
		if (isEndOfText()) {
			return error("Unexpected end of input");
		}
		return error("Expected " + expected);
	}




	/**
	 * generate a parse exception with the given message.
	 * @param message
	 * @return
	 */
	private ParseException error(final String message) {
		final int absIndex = bufferOffset + index;
		final int column = absIndex - lineOffset;
		final int offset = isEndOfText() ? absIndex : absIndex - 1;
		return new ParseException(message, offset, line, column, current);
	}




	private boolean isWhiteSpace() {
		return (current == ' ') || (current == '\t') || (current == '\n') || (current == '\r');
	}




	private boolean isDigit() {
		return (current >= '0') && (current <= '9');
	}




	private boolean isHexDigit() {
		return ((current >= '0') && (current <= '9')) || ((current >= 'a') && (current <= 'f')) || ((current >= 'A') && (current <= 'F'));
	}




	private boolean isEndOfText() {
		return current == -1;
	}

}
