package au.com.codeka.carrot.tmpl.parse;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

import javax.annotation.Nullable;

import com.google.common.io.LineReader;

import au.com.codeka.carrot.CarrotException;

/**
 * The {@link Tokenizer} takes an input stream of character and turns it into a
 * stream of {@link Token}s.
 *
 * <p>
 * Each {@link Token} represents a high-level component of the template. For
 * example, the following template:
 *
 * <p>
 * <code>Text {{ hello }} stuff {% if (blah) %} more stuff {% end %}</code>
 *
 * <p>
 * Corresponds to the following stream of tokens:
 *
 * <pre>
 * <code>TokenType=FIXED, Content="Text "
 *TokenType=ECHO, Content=" hello "
 *TokenType=FIXED, Content=" stuff "
 *TokenType=TAG, Content=" if (blah) "
 *TokenType=FIXED, Content=" more stuff "
 *TokenType=TAG, Content=" end "</code>
 * </pre>
 */
public class Tokenizer {

	private final PushbackReader reader;

	/**
	 * Construct a new {@link Tokenizer} with the given {@link LineReader}, and
	 * a default {@link TokenFactory}.
	 *
	 * @param reader
	 */
	public Tokenizer(Reader reader) {
		this.reader = new PushbackReader(reader, 1);
	}

	/**
	 * Gets the next token from the stream, or null if there's no tokens left.
	 *
	 * @return The next {@link Token} in the stream, or null if we're at the end
	 *         of the stream.
	 * @throws CarrotException when there's an error parsing the tokens.
	 */
	@Nullable
	public Token getNextToken() throws IOException, CarrotException {
		Token token;
		do {
			token = mode.parse(this);
		} while (token != null && token.getValue().isEmpty());
		return token;
	}

	private interface ParseMode {

		Token parse(Tokenizer tokenizer) throws IOException;
	}

	private static class TagParseMode implements ParseMode {

		private final char end;
		private final TokenType type;

		private TagParseMode(char end, TokenType type) {
			this.end = end;
			this.type = type;
		}

		@Override
		public Token parse(Tokenizer tokenizer) throws IOException {
			return new Token(type, tokenizer.parseTag(end));
		}
	}

	private static final ParseMode TAG = new TagParseMode('%', TokenType.TAG);
	private static final ParseMode ECHO = new TagParseMode('}', TokenType.ECHO);
	private static final ParseMode COMMENT = new TagParseMode('#', TokenType.COMMENT);

	private static final ParseMode LITERAL = new ParseMode() {

		@Override
		public Token parse(Tokenizer tokenizer) throws IOException {
			return new Token(TokenType.FIXED, tokenizer.parseLiteral());
		}
	};

	private static final ParseMode END = new ParseMode() {

		@Override
		public Token parse(Tokenizer tokenizer) throws IOException {
			return null;
		}
	};

	private ParseMode mode = LITERAL;

	private String parseTag(char end) throws IOException {
		StringBuilder content = new StringBuilder();
		for (;;) {
			int c = reader.read();
			if (c == end) {
				int c2 = reader.read();
				if (c2 == '}') {
					return finishToken(content, LITERAL);
				}
				reader.unread(c2);
			} else if (c == -1) {
				return finishToken(content, END);
			}
			content.append((char) c);
		}
	}

	private String parseLiteral() throws IOException {
		StringBuilder content = new StringBuilder();
		for (;;) {
			int c = reader.read();
			switch (c) {
				case -1:
					return finishToken(content, END);
				case '{':
					int c2 = reader.read();
					switch (c2) {
						case -1:
							return finishToken(content, END);
						case '{':
							return finishToken(content, ECHO);
						case '%':
							return finishToken(content, TAG);
						case '#':
							return finishToken(content, COMMENT);
						default:
							reader.unread(c2);
					}
				default:
					content.append((char) c);
			}
		}
	}

	private String finishToken(StringBuilder content, ParseMode newMode) {
		this.mode = newMode;
		return content.toString();
	}

}
