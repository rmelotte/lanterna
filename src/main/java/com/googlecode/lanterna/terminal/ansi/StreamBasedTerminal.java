/*
 * This file is part of lanterna (http://code.google.com/p/lanterna/).
 *
 * lanterna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2010-2014 Martin
 */
package com.googlecode.lanterna.terminal.ansi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import com.googlecode.lanterna.LanternaException;
import com.googlecode.lanterna.input.InputDecoder;
import com.googlecode.lanterna.terminal.ACS;
import com.googlecode.lanterna.terminal.InputEnabledAbstractTerminal;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

/**
 * An abstract terminal implementing functionality for terminals using OutputStream/InputStream
 *
 * @author Martin
 */
public abstract class StreamBasedTerminal extends InputEnabledAbstractTerminal {

    private static Charset UTF8_REFERENCE;

    static {
        try {
            UTF8_REFERENCE = Charset.forName("UTF-8");
        }
        catch(Exception e) {
            UTF8_REFERENCE = null;
        }
    }

    private final InputStream terminalInput;
    private final OutputStream terminalOutput;
    private final Charset terminalCharset;

    public StreamBasedTerminal(InputStream terminalInput, OutputStream terminalOutput, Charset terminalCharset) {
        super(new InputDecoder(new InputStreamReader(terminalInput, terminalCharset)));
        this.terminalInput = terminalInput;
        this.terminalOutput = terminalOutput;
        if(terminalCharset == null) {
            this.terminalCharset = Charset.defaultCharset();
        }
        else {
            this.terminalCharset = terminalCharset;
        }
    }

    /**
     * Outputs a single character to the terminal output stream, translating any UTF-8 graphical symbol if necessary
     *
     * @param c Character to write to the output stream
     * @throws LanternaException
     */
    @Override
    public void putCharacter(char c) throws IOException {
        writeToTerminal(translateCharacter(c));
    }

    /**
     * This method will write a list of bytes directly to the output stream of the terminal.
     */
    protected void writeToTerminal(byte... bytes) throws IOException {
        synchronized(terminalOutput) {
            terminalOutput.write(bytes);
        }
    }

    @Override
    public byte[] enquireTerminal(int timeout, TimeUnit timeoutTimeUnit) throws IOException {
        synchronized(terminalOutput) {
            terminalOutput.write(5);    //ENQ
            flush();
        }
        
        //Wait for input
        long startTime = System.currentTimeMillis();
        while(terminalInput.available() == 0) {
            if(System.currentTimeMillis() - startTime > timeoutTimeUnit.toMillis(timeout)) {
                return new byte[0];
            }
            try { 
                Thread.sleep(1); 
            } 
            catch(InterruptedException e) {
                return new byte[0];
            }
        }
        
        //We have at least one character, read as far as we can and return
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while(terminalInput.available() > 0) {
            baos.write(terminalInput.read());
        }
        return baos.toByteArray();
    }

    @Override
    public void flush() throws IOException {
        synchronized(terminalOutput) {
            terminalOutput.flush();
        }
    }

    protected byte[] translateCharacter(char input) {
        if(UTF8_REFERENCE != null && UTF8_REFERENCE == terminalCharset) {
            return convertToCharset(input);
        }
        //Convert ACS to ordinary terminal codes
        switch(input) {
            case ACS.ARROW_DOWN:
                return convertToVT100('v');
            case ACS.ARROW_LEFT:
                return convertToVT100('<');
            case ACS.ARROW_RIGHT:
                return convertToVT100('>');
            case ACS.ARROW_UP:
                return convertToVT100('^');
            case ACS.BLOCK_DENSE:
            case ACS.BLOCK_MIDDLE:
            case ACS.BLOCK_SOLID:
            case ACS.BLOCK_SPARSE:
                return convertToVT100((char) 97);
            case ACS.HEART:
            case ACS.CLUB:
            case ACS.SPADES:
                return convertToVT100('?');
            case ACS.FACE_BLACK:
            case ACS.FACE_WHITE:
            case ACS.DIAMOND:
                return convertToVT100((char) 96);
            case ACS.DOT:
                return convertToVT100((char) 102);
            case ACS.DOUBLE_LINE_CROSS:
            case ACS.SINGLE_LINE_CROSS:
                return convertToVT100((char) 110);
            case ACS.DOUBLE_LINE_HORIZONTAL:
            case ACS.SINGLE_LINE_HORIZONTAL:
                return convertToVT100((char) 113);
            case ACS.DOUBLE_LINE_LOW_LEFT_CORNER:
            case ACS.SINGLE_LINE_LOW_LEFT_CORNER:
                return convertToVT100((char) 109);
            case ACS.DOUBLE_LINE_LOW_RIGHT_CORNER:
            case ACS.SINGLE_LINE_LOW_RIGHT_CORNER:
                return convertToVT100((char) 106);
            case ACS.DOUBLE_LINE_T_DOWN:
            case ACS.SINGLE_LINE_T_DOWN:
            case ACS.DOUBLE_LINE_T_SINGLE_DOWN:
            case ACS.SINGLE_LINE_T_DOUBLE_DOWN:
                return convertToVT100((char) 119);
            case ACS.DOUBLE_LINE_T_LEFT:
            case ACS.SINGLE_LINE_T_LEFT:
            case ACS.DOUBLE_LINE_T_SINGLE_LEFT:
            case ACS.SINGLE_LINE_T_DOUBLE_LEFT:
                return convertToVT100((char) 117);
            case ACS.DOUBLE_LINE_T_RIGHT:
            case ACS.SINGLE_LINE_T_RIGHT:
            case ACS.DOUBLE_LINE_T_SINGLE_RIGHT:
            case ACS.SINGLE_LINE_T_DOUBLE_RIGHT:
                return convertToVT100((char) 116);
            case ACS.DOUBLE_LINE_T_UP:
            case ACS.SINGLE_LINE_T_UP:
            case ACS.DOUBLE_LINE_T_SINGLE_UP:
            case ACS.SINGLE_LINE_T_DOUBLE_UP:
                return convertToVT100((char) 118);
            case ACS.DOUBLE_LINE_UP_LEFT_CORNER:
            case ACS.SINGLE_LINE_UP_LEFT_CORNER:
                return convertToVT100((char) 108);
            case ACS.DOUBLE_LINE_UP_RIGHT_CORNER:
            case ACS.SINGLE_LINE_UP_RIGHT_CORNER:
                return convertToVT100((char) 107);
            case ACS.DOUBLE_LINE_VERTICAL:
            case ACS.SINGLE_LINE_VERTICAL:
                return convertToVT100((char) 120);
            default:
                return convertToCharset(input);
        }
    }

    private byte[] convertToVT100(char code) {
        //Warning! This might be terminal type specific!!!!
        //So far it's worked everywhere I've tried it (xterm, gnome-terminal, putty)
        return new byte[]{27, 40, 48, (byte) code, 27, 40, 66};
    }

    private byte[] convertToCharset(char input) {
        //TODO: This is a silly way to do it, improve?
        final char[] buffer = new char[1];
        buffer[0] = input;
        return terminalCharset.encode(CharBuffer.wrap(buffer)).array();
    }
}