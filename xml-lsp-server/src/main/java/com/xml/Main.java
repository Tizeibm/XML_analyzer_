package com.xml;

import com.xml.handlers.LargeXmlValidator;

import java.io.File;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("enter the path of xml file:");
        String xmlfile = scanner.nextLine();
        System.out.println("enter the path of xsd file:");
        String xsdfile = scanner.nextLine();

        LargeXmlValidator largeXmlValidator = new LargeXmlValidator();
        largeXmlValidator.validateWithoutZones(new File(xmlfile), new File(xsdfile));
    }
}
