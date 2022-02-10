/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.util;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.options.SerializeOptions;
import com.android.camera.debug.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.sprd.camera.storagepath.ExternalStorageUtil;


import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.util.Properties;


import com.adobe.xmp.impl.*;
import com.adobe.xmp.options.PropertyOptions;
import com.adobe.xmp.impl.xpath.XMPPath;
import com.adobe.xmp.XMPError;
import com.adobe.xmp.impl.xpath.XMPPathParser;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


/**
 * Util class to read/write xmp from a jpeg image file. It only supports jpeg
 * image format, and doesn't support extended xmp now.
 * To use it:
 * XMPMeta xmpMeta = XmpUtil.extractOrCreateXMPMeta(filename);
 * xmpMeta.setProperty(PanoConstants.GOOGLE_PANO_NAMESPACE, "property_name", "value");
 * XmpUtil.writeXMPMeta(filename, xmpMeta);
 *
 * Or if you don't care the existing XMP meta data in image file:
 * XMPMeta xmpMeta = XmpUtil.createXMPMeta();
 * xmpMeta.setPropertyBoolean(PanoConstants.GOOGLE_PANO_NAMESPACE, "bool_property_name", "true");
 * XmpUtil.writeXMPMeta(filename, xmpMeta);
 */
public class XmpUtil {
  private static final Log.Tag TAG = new Log.Tag("XmpUtil");
  private static final int XMP_HEADER_SIZE = 29;
  private static final String XMP_HEADER = "http://ns.adobe.com/xap/1.0/\0";
  private static final int MAX_XMP_BUFFER_SIZE = 65502;

  private static final String GOOGLE_PANO_NAMESPACE = "http://ns.google.com/photos/1.0/panorama/";
  private static final String PANO_PREFIX = "GPano";

  private static final int M_SOI = 0xd8; // File start marker.
  private static final int M_APP1 = 0xe1; // Marker for Exif or XMP.
  private static final int M_SOS = 0xda; // Image data marker.

  // Jpeg file is composed of many sections and image data. This class is used
  // to hold the section data from image file.
  private static class Section {
    public int marker;
    public int length;
    public byte[] data;
  }

  static {
    try {
      XMPMetaFactory.getSchemaRegistry().registerNamespace(
          GOOGLE_PANO_NAMESPACE, PANO_PREFIX);
    } catch (XMPException e) {
      e.printStackTrace();
    }
  }

  /**
   * Extracts XMPMeta from JPEG image file.
   *
   * @param filename JPEG image file name.
   * @return Extracted XMPMeta or null.
   */
  public static XMPMeta extractXMPMeta(String filename) {
    if (!filename.toLowerCase().endsWith(".jpg")
        && !filename.toLowerCase().endsWith(".jpeg")) {
      Log.d(TAG, "XMP parse: only jpeg file is supported");
      return null;
    }

    try {
      return extractXMPMeta(new FileInputStream(filename));
    } catch (FileNotFoundException e) {
      Log.e(TAG, "Could not read file: " + filename, e);
      return null;
    }
  }

  /**
   *  Extracts XMPMeta from a JPEG image file stream.
   *
   * @param is the input stream containing the JPEG image file.
   * @return Extracted XMPMeta or null.
   */
  public static XMPMeta extractXMPMeta(InputStream is) {
    List<Section> sections = parse(is, true);
    if (sections == null) {
      return null;
    }
    // Now we don't support extended xmp.
    for (Section section : sections) {
      if (hasXMPHeader(section.data)) {
        int end = getXMPContentEnd(section.data);
        byte[] buffer = new byte[end - XMP_HEADER_SIZE];
        System.arraycopy(
            section.data, XMP_HEADER_SIZE, buffer, 0, buffer.length);
        try {
          XMPMeta result = XMPMetaFactory.parseFromBuffer(buffer);
          return result;
        } catch (XMPException e) {
          Log.d(TAG, "XMP parse error", e);
          return null;
        }
      }
    }
    return null;
  }

  /**
   * Creates a new XMPMeta.
   */
  public static XMPMeta createXMPMeta() {
    return XMPMetaFactory.create();
  }

  /**
   * Tries to extract XMP meta from image file first, if failed, create one.
   */
  public static XMPMeta extractOrCreateXMPMeta(String filename) {
    XMPMeta meta = extractXMPMeta(filename);
    return meta == null ? createXMPMeta() : meta;
  }

  /**
   * Writes the XMPMeta to the jpeg image file.
   */
  public static boolean writeXMPMeta(String filename, XMPMeta meta) {
    if (!filename.toLowerCase().endsWith(".jpg")
        && !filename.toLowerCase().endsWith(".jpeg")) {
      Log.d(TAG, "XMP parse: only jpeg file is supported");
      return false;
    }
    List<Section> sections = null;
    try {
      sections = parse(new FileInputStream(filename), false);
      sections = insertXMPSection(sections, meta);
      if (sections == null) {
        return false;
      }
    } catch (FileNotFoundException e) {
      Log.e(TAG, "Could not read file: " + filename, e);
      return false;
    }
    FileOutputStream os = null;
    try {
      // Overwrite the image file with the new meta data.
      os = new FileOutputStream(filename);
      writeJpegFile(os, sections);
    } catch (IOException e) {
      Log.d(TAG, "Write file failed:" + filename, e);
      return false;
    } finally {
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
          // Ignore.
        }
      }
    }
    return true;
  }

  /**
   * Updates a jpeg file from inputStream with XMPMeta to outputStream.
   */
  public static boolean writeXMPMeta(InputStream inputStream, OutputStream outputStream,
      XMPMeta meta) {
    List<Section> sections = parse(inputStream, false);
      sections = insertXMPSection(sections, meta);
      if (sections == null) {
        return false;
      }
    try {
      // Overwrite the image file with the new meta data.
      writeJpegFile(outputStream, sections);
    } catch (IOException e) {
      Log.d(TAG, "Write to stream failed", e);
      return false;
    } finally {
      if (outputStream != null) {
        try {
          outputStream.close();
        } catch (IOException e) {
          // Ignore.
        }
      }
    }
    return true;
  }

  public static boolean writeXMPMeta(ContentResolver cr, Uri uri, XMPMeta meta) {
    ExternalStorageUtil externalStorageUtil = ExternalStorageUtil.getInstance();
    ParcelFileDescriptor pfd = null;
    FileOutputStream fileOutputStream = null;
    try {
      pfd = cr.openFileDescriptor(uri, "r");
      FileInputStream fileInputStream = externalStorageUtil.createInputStream(pfd);
      List<Section> sections = parse(fileInputStream, false);
      pfd.close();
      fileInputStream.close();
      sections = insertXMPSection(sections, meta);
      if (sections == null) {
        return false;
      }
      pfd = cr.openFileDescriptor(uri, "w");
      fileOutputStream = externalStorageUtil.createOutputStream(pfd);
      // Overwrite the image file with the new meta data.
      writeJpegFile(fileOutputStream, sections);
    } catch (Exception e) {
      Log.d(TAG, "Write to stream failed", e);
      return false;
    } finally {
      if (fileOutputStream != null) {
        try {
          pfd.close();
          fileOutputStream.close();
        } catch (IOException e) {
          // Ignore.
        }
      }
    }
    return  true;
  }

  /**
   * Write a list of sections to a Jpeg file.
   */
  private static void writeJpegFile(OutputStream os, List<Section> sections)
      throws IOException {
    // Writes the jpeg file header.
    os.write(0xff);
    os.write(M_SOI);
    for (Section section : sections) {
      os.write(0xff);
      os.write(section.marker);
      if (section.length > 0) {
        // It's not the image data.
        int lh = section.length >> 8;
        int ll = section.length & 0xff;
        os.write(lh);
        os.write(ll);
      }
      os.write(section.data);
    }
  }

  private static List<Section> insertXMPSection(
      List<Section> sections, XMPMeta meta) {
    if (sections == null || sections.size() <= 1) {
      return null;
    }
    byte[] buffer;
    try {
      SerializeOptions options = new SerializeOptions();
      options.setUseCompactFormat(true);
      // We have to omit packet wrapper here because
      // javax.xml.parsers.DocumentBuilder
      // fails to parse the packet end <?xpacket end="w"?> in android.
      options.setOmitPacketWrapper(true);
      buffer = XMPMetaFactory.serializeToBuffer(meta, options);
      buffer = removeParseType(buffer);
    } catch (XMPException e) {
      Log.d(TAG, "Serialize xmp failed", e);
      return null;
    }
    if (buffer.length > MAX_XMP_BUFFER_SIZE) {
      // Do not support extended xmp now.
      return null;
    }
    // The XMP section starts with XMP_HEADER and then the real xmp data.
    byte[] xmpdata = new byte[buffer.length + XMP_HEADER_SIZE];
    System.arraycopy(XMP_HEADER.getBytes(), 0, xmpdata, 0, XMP_HEADER_SIZE);
    System.arraycopy(buffer, 0, xmpdata, XMP_HEADER_SIZE, buffer.length);
    Section xmpSection = new Section();
    xmpSection.marker = M_APP1;
    // Adds the length place (2 bytes) to the section length.
    xmpSection.length = xmpdata.length + 2;
    xmpSection.data = xmpdata;

    for (int i = 0; i < sections.size(); ++i) {
      // If we can find the old xmp section, replace it with the new one.
      if (sections.get(i).marker == M_APP1
          && hasXMPHeader(sections.get(i).data)) {
        // Replace with the new xmp data.
        sections.set(i, xmpSection);
        return sections;
      }
    }
    // If the first section is Exif, insert XMP data before the second section,
    // otherwise, make xmp data the first section.
    List<Section> newSections = new ArrayList<Section>();
    int position = (sections.get(0).marker == M_APP1) ? 1 : 0;
    newSections.addAll(sections.subList(0, position));
    newSections.add(xmpSection);
    newSections.addAll(sections.subList(position, sections.size()));
    return newSections;
  }

    private static byte[] removeParseType(byte[] buffer) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbFactory.newDocumentBuilder();
            ByteArrayInputStream stream = new ByteArrayInputStream(buffer);
            Document document = db.parse(stream);


            NodeList nodeList = document.getElementsByTagName("rdf:li");
            for(int i = 0; i<nodeList.getLength(); i++ ){
                Element node = (Element) nodeList.item(i);
                node.removeAttribute("rdf:parseType");
            }

            TransformerFactory tf   =   TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
//            t.setOutputProperty("encoding","GB23121");//解决中文问题，试过用GBK不行
            ByteArrayOutputStream   bos   =   new   ByteArrayOutputStream();
            t.transform(new DOMSource(document.getElementsByTagName("x:xmpmeta").item(0)), new StreamResult(bos));
            String xmlStr = bos.toString();
            final String remove = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
            String result = xmlStr.substring(remove.length());
            return result.getBytes();

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return buffer;
    }

    /**
   * Checks whether the byte array has XMP header. The XMP section contains
   * a fixed length header XMP_HEADER.
   *
   * @param data Xmp metadata.
   */
  private static boolean hasXMPHeader(byte[] data) {
    if (data.length < XMP_HEADER_SIZE) {
      return false;
    }
    try {
      byte[] header = new byte[XMP_HEADER_SIZE];
      System.arraycopy(data, 0, header, 0, XMP_HEADER_SIZE);
      if (new String(header, "UTF-8").equals(XMP_HEADER)) {
        return true;
      }
    } catch (UnsupportedEncodingException e) {
      return false;
    }
    return false;
  }

  /**
   * Gets the end of the xmp meta content. If there is no packet wrapper,
   * return data.length, otherwise return 1 + the position of last '>'
   * without '?' before it.
   * Usually the packet wrapper end is "<?xpacket end="w"?> but
   * javax.xml.parsers.DocumentBuilder fails to parse it in android.
   *
   * @param data xmp metadata bytes.
   * @return The end of the xmp metadata content.
   */
  private static int getXMPContentEnd(byte[] data) {
    for (int i = data.length - 1; i >= 1; --i) {
      if (data[i] == '>') {
        if (data[i - 1] != '?') {
          return i + 1;
        }
      }
    }
    // It should not reach here for a valid xmp meta.
    return data.length;
  }

  /**
   * Parses the jpeg image file. If readMetaOnly is true, only keeps the Exif
   * and XMP sections (with marker M_APP1) and ignore others; otherwise, keep
   * all sections. The last section with image data will have -1 length.
   *
   * @param is Input image data stream.
   * @param readMetaOnly Whether only reads the metadata in jpg.
   * @return The parse result.
   */
  private static List<Section> parse(InputStream is, boolean readMetaOnly) {
    try {
      if (is.read() != 0xff || is.read() != M_SOI) {
        return null;
      }
      List<Section> sections = new ArrayList<Section>();
      int c;
      while ((c = is.read()) != -1) {
        if (c != 0xff) {
          return null;
        }
        // Skip padding bytes.
        while ((c = is.read()) == 0xff) {
        }
        if (c == -1) {
          return null;
        }
        int marker = c;
        if (marker == M_SOS) {
          // M_SOS indicates the image data will follow and no metadata after
          // that, so read all data at one time.
          if (!readMetaOnly) {
            Section section = new Section();
            section.marker = marker;
            section.length = -1;
            section.data = new byte[is.available()];
            is.read(section.data, 0, section.data.length);
            sections.add(section);
          }
          return sections;
        }
        int lh = is.read();
        int ll = is.read();
        if (lh == -1 || ll == -1) {
          return null;
        }
        int length = lh << 8 | ll;
        if(length <= 2){
          return null;
        }
        if (!readMetaOnly || c == M_APP1) {
          Section section = new Section();
          section.marker = marker;
          section.length = length;
          section.data = new byte[length - 2];
          is.read(section.data, 0, length - 2);
          sections.add(section);
        } else {
          // Skip this section since all exif/xmp meta will be in M_APP1
          // section.
          is.skip(length - 2);
        }
      }
      return sections;
    } catch (IOException e) {
      Log.d(TAG, "Could not parse file.", e);
      return null;
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          // Ignore.
        }
      }
    }
  }

  public static byte[] readFile(String path) {
    File file = new File(path);
    if (file.isFile()) {
      FileInputStream fis = null;
      ByteArrayOutputStream outputStream = null;
      try {
        fis = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        outputStream = new ByteArrayOutputStream();
        int len = 0;
        while ((len = fis.read(buffer)) != -1) {
          outputStream.write(buffer, 0, len);
        }
        byte[] result = outputStream.toByteArray();
        return result;
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          if(fis != null){
            fis.close();
          }

          if(outputStream != null){
            outputStream.close();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    } else { System.out.println("file not exist");
    }
    return null;
  }

  public static void saveBytesToFile(String filePath, byte[] data) {
    File file = new File(filePath);
    BufferedOutputStream outStream = null;
    try {
      outStream = new BufferedOutputStream(new FileOutputStream(file));
      outStream.write(data);
      outStream.flush();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (null != outStream) {
        try {
          outStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  //this is for add array of struct
//  public void appendArrayItem(String schemaNS, String arrayName, PropertyOptions arrayOptions,String structName, String fieldNS,
//                              String fieldName, String itemValue, PropertyOptions itemOptions) throws XMPException {
////    ParameterAsserts.assertSchemaNS(schemaNS);
////    ParameterAsserts.assertArrayName(arrayName);
//    if (arrayOptions == null) {
//      arrayOptions = new PropertyOptions();
//    }
//    if (!arrayOptions.isOnlyArrayOptions()) {
//      throw new XMPException("Only array form flags allowed for arrayOptions", XMPError.BADOPTIONS);
//    }
//    // Check if array options are set correctly.
//    arrayOptions = verifySetOptions(arrayOptions, null);
//    // Locate or create the array. If it already exists, make sure the array
//    // form from the options
//    // parameter is compatible with the current state.
//    XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, arrayName);
//    // Just lookup, don't try to create.
//    XMPNode arrayNode = XMPNodeUtils.findNode(tree, arrayPath, false, null);
//    if (arrayNode != null) {
//      // The array exists, make sure the form is compatible. Zero
//      // arrayForm means take what exists.
//      if (!arrayNode.getOptions().isArray()) {
//        throw new XMPException("The named property is not an array", XMPError.BADXPATH);
//      }
//      // if (arrayOptions != null && !arrayOptions.equalArrayTypes(arrayNode.getOptions()))
//      // {
//      // throw new XMPException("Mismatch of existing and specified array form", BADOPTIONS);
//      // }
//      } else {
//      // The array does not exist, try to create it.
//      if (arrayOptions.isArray()) {
//        arrayNode = XMPNodeUtils.findNode(tree, arrayPath, true, arrayOptions);
//        if (arrayNode == null) {
//          throw new XMPException("Failure creating array node", XMPError.BADXPATH);
//        }
//      } else {
//        // array options missing
//        throw new XMPException("Explicit arrayOptions required to create new array", XMPError.BADOPTIONS);
//      }
//    }
//    doSetArrayItem(arrayNode, -1, structName, fieldNS, fieldName,itemValue, itemOptions, true);
//  }
//
//	private void doSetArrayItem(XMPNode arrayNode, int itemIndex, String structName, String fieldNS,
//                                String fieldName,String itemValue, PropertyOptions itemOptions, boolean insert) throws XMPException {
//      String fieldPath = structName + XMPPathFactory.composeStructFieldPath(fieldNS, fieldName);
//      XMPPath expPath = XMPPathParser.expandXPath(schemaNS, fieldPath);
//      XMPNode itemNode = XMPNodeUtils.findNode(tree, expPath, true, options);
//
//    //XMPNode itemNode = new XMPNode(ARRAY_ITEM_NAME, null);
//    itemOptions = verifySetOptions(itemOptions, itemValue);
//       // in insert mode the index after the last is allowed,
//      // even ARRAY_LAST_ITEM points to the index *after* the last.
//      int maxIndex = insert ? arrayNode.getChildrenLength() + 1 : arrayNode.getChildrenLength();
//      if (itemIndex == -1) {
//        itemIndex = maxIndex;
//      }
//
//      if (1 <= itemIndex && itemIndex <= maxIndex) {
//        if (!insert) {
//          arrayNode.removeChild(itemIndex);
//        }
//        arrayNode.addChild(itemIndex, itemNode);
//        XMPMeta.setNode(itemNode, itemValue, itemOptions, false);
//      } else {
//        throw new XMPException("Array index out of bounds", XMPError.BADINDEX);
//      }
//  }
//
//  static PropertyOptions verifySetOptions(PropertyOptions options, Object itemValue)
//			throws XMPException
//{
//  		// create empty and fix existing options
//   	if (options == null)
//      	{
//   		// set default options
//   		options = new PropertyOptions();
//  	}
//
//	if (options.isArrayAltText())
//  	{
//  			options.setArrayAlternate(true);
// 	}
//
//	if (options.isArrayAlternate())
//	{
//			options.setArrayOrdered(true);
//		}
//
// 	if (options.isArrayOrdered())
//    		{
//   		options.setArray(true);
//     	}
//
//if (options.isCompositeProperty() && itemValue != null && itemValue.toString().length() > 0)
//      		{
//   		throw new XMPException("Structs and arrays can't have values",
//            				XMPError.BADOPTIONS);
//   		}
//
//   	options.assertConsistency(options.getOptions());
//
//	return options;
// }
  //this is for struct
//  public void setStructField(XMPMeta data, String schemaNS, String structName, String fieldNS,
//			String fieldName, String fieldValue, PropertyOptions options) throws XMPException {
//    ParameterAsserts.assertSchemaNS(schemaNS);
// 	ParameterAsserts.assertStructName(structName);
//    String fieldPath = structName + XMPPathFactory.composeStructFieldPath(fieldNS, fieldName);
//    XMPNode tree = data.getRoot();
//    setProperty(tree, schemaNS, fieldPath, fieldValue, options);
//  }
//  public void setProperty(XMPNode tree, String schemaNS, String propName, Object propValue,
//			PropertyOptions options) throws XMPException {
//    ParameterAsserts.assertSchemaNS(schemaNS);
//    ParameterAsserts.assertPropName(propName);
//    options = XMPNodeUtils.verifySetOptions(options, propValue);
//    XMPPath expPath = XMPPathParser.expandXPath(schemaNS, propName);
//    XMPNode propNode = XMPNodeUtils.findNode(tree, expPath, true, options);
//    if (propNode != null) {
//      setNode(propNode, propValue, options, false);
//    } else {
//      throw new XMPException("Specified property does not exist", XMPError.BADXPATH);}
//  }

  private XmpUtil() {}
}
