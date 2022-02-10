package com.android.camera.util;

import com.adobe.xmp.XMPConst;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.XMPUtils;
import com.adobe.xmp.impl.XMPSerializerRDF;
import com.adobe.xmp.impl.xpath.XMPPath;
import com.adobe.xmp.options.PropertyOptions;
import com.android.camera.debug.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.adobe.xmp.options.SerializeOptions;
import com.adobe.xmp.XMPPathFactory;

/**
 * Generates XMP according to the Photos OEM exif spec.
 */
public final class XmpBuilder {

  private String specialTypeId;
  private String burstId;
  private Boolean isPrimaryInBurst;
  private Set<AutoCreation> disabledAutoCreations;

  //
  private long motionPhotoPresentationTimestampUs = -1L; // it >= 0 means MotionPhoto , Bug 1169067
  private long motionPhotoVideoLength;
  private int motionPhotoPaddingLength;


  public XmpBuilder setMPTimeStampUs(long timeStampUs) {
    this.motionPhotoPresentationTimestampUs = timeStampUs;
    return this;
  }

  public XmpBuilder setMPVideoLength (long videoLength) {
    this.motionPhotoVideoLength = videoLength;
    return this;
  }

  public XmpBuilder setMPPaddingLength(int length) {
    this.motionPhotoPaddingLength = length;
    return this;
  }

  public XmpBuilder setSpecialTypeId(String specialTypeId) {
    this.specialTypeId = specialTypeId;
    return this;
  }

  public XmpBuilder setBurstId(String burstId) {
    this.burstId = burstId;
    return this;
  }

  public XmpBuilder setIsPrimaryInBurst(boolean isPrimaryInBurst) {
    this.isPrimaryInBurst = isPrimaryInBurst;
    return this;
  }

  public XmpBuilder addDisableAutoCreations(AutoCreation... autoCreations) {
    if (disabledAutoCreations == null) {
      disabledAutoCreations = new HashSet<>();
    }
    disabledAutoCreations.addAll(Arrays.asList(autoCreations));
    return this;
  }

  public XmpBuilder setDisableAutoCreation(AutoCreation... autoCreations) {
    disabledAutoCreations = new HashSet<>(Arrays.asList(autoCreations));
    return this;
  }

  public XMPMeta build() throws XMPException {
    XMPMeta xmpMeta = XMPMetaFactory.create();
    if (motionPhotoPresentationTimestampUs >= 0) { // Bug 1169067
      XMPMetaFactory.getSchemaRegistry()
              .registerNamespace(XmpConstants.NAMESPACE_URI, XmpConstants.CAMERA_NAMESPACE_PREFIX);
      XMPMetaFactory.getSchemaRegistry()
              .registerNamespace(XmpConstants.CONTAINER_NAMESPACE_URI, XmpConstants.CONTAINER_NAMESPACE_PREFIX);
      XMPMetaFactory.getSchemaRegistry()
              .registerNamespace(XmpConstants.ITEM_NAMESPACE_URI, XmpConstants.ITEM_NAMESPACE_PREFIX);
    } else {
      XMPMetaFactory.getSchemaRegistry()
              .registerNamespace(XmpConstants.NAMESPACE_URI, XmpConstants.NAMESPACE_PREFIX);
    }
    if (specialTypeId != null) {
      setProperty(xmpMeta, XmpConstants.PROPERTY_SPECIAL_TYPE_ID, specialTypeId);
    }
    if (burstId != null) {
      setProperty(xmpMeta, XmpConstants.PROPERTY_BURST_ID, burstId);
    }
    if (isPrimaryInBurst != null) {
      setProperty(xmpMeta, XmpConstants.PROPERTY_BURST_PRIMARY,
          String.valueOf(isPrimaryInBurst ? 1 : 0));
    }
    if (disabledAutoCreations != null) {
      for (AutoCreation current : disabledAutoCreations) {
        PropertyOptions arrayOptions = new PropertyOptions()
            .setArray(true)
            .setArrayOrdered(false);
        xmpMeta.appendArrayItem(XmpConstants.NAMESPACE_URI,
            XmpConstants.PROPERTY_DISABLE_AUTO_CREATION, arrayOptions, current.getValue(),
            null /*itemOptions*/);
      }
    }
    if (motionPhotoPresentationTimestampUs >= 0) { // Bug 1169067
      //insert motion photo basic info
      xmpMeta.setProperty(XmpConstants.NAMESPACE_URI,XmpConstants.PROPERTY_MOTIONPHOTO,1);
      xmpMeta.setProperty(XmpConstants.NAMESPACE_URI,XmpConstants.PROPERTY_MOTIONPHOTO_VERSION,1);
      xmpMeta.setProperty(XmpConstants.NAMESPACE_URI,XmpConstants.PROPERTY_MOTIONPHOTO_PRESENTATION_TIMESTAMP_US,motionPhotoPresentationTimestampUs);
    }

    if (motionPhotoVideoLength != 0 && motionPhotoPresentationTimestampUs >= 0) { // Bug 1169067
//      // 10
//      PropertyOptions arrayOptions = new PropertyOptions();
//      arrayOptions.setArray(true);
//      arrayOptions.setArrayOrdered(true);
//      PropertyOptions structOptionstrue = new PropertyOptions();
//      structOptionstrue.setStruct(true);
//
//        PropertyOptions structOptionstrue1 = new PropertyOptions();
//        structOptionstrue1.setStruct(true);
//        structOptionstrue1.setQualifier(true);
//
//
//        String rdfType = XMPPathFactory.composeStructFieldPath(XMPConst.NS_RDF,XMPConst.RDF_TYPE);
////        String rdfValue = XMPPathFactory.composeStructFieldPath(XMPConst.NS_RDF,"value");
//        String rdfValue =  XMPPathFactory.composeQualifierPath(XMPConst.NS_RDF,"value");
//
//        // array
//        xmpMeta.appendArrayItem(XmpConstants.CONTAINER_NAMESPACE_URI,"Directory",arrayOptions,null,structOptionstrue);
//        String itemPath1 = XMPPathFactory.composeArrayItemPath("Directory",1);
//
////        String item1RdfType = itemPath1 + rdfType;
//        String item1RdfValue = itemPath1 + rdfValue;
//
//
////        //insert primary pic info
//        xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,itemPath1 ,XMPConst.NS_RDF,
//                "value",null,structOptionstrue);
////        xmpMeta.setQualifier(XmpConstants.CONTAINER_NAMESPACE_URI,itemPath1 ,XMPConst.NS_RDF,
////                "value",null,structOptionstrue);
//
//        xmpMeta.setQualifier(XmpConstants.CONTAINER_NAMESPACE_URI,item1RdfValue,XmpConstants.ITEM_NAMESPACE_URI,
//                "Semantic","Primary");
//        xmpMeta.setQualifier(XmpConstants.CONTAINER_NAMESPACE_URI,item1RdfValue,XmpConstants.ITEM_NAMESPACE_URI,
//                "Mime","image/jpeg");
//
//
////      xmpMeta.appendArrayItem(XmpConstants.CONTAINER_NAMESPACE_URI,"Directory",arrayOptions,null,structOptionstrue);
////      String itemPath2 = XMPPathFactory.composeArrayItemPath("Directory",2);
////        String item2RdfType = itemPath2 + rdfType;
////        String item2RdfValue = itemPath2 + rdfValue;
////
//
////      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,item1RdfValue,XmpConstants.ITEM_NAMESPACE_URI,
////              "Semantic","Primary");
////      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,item1RdfValue,XmpConstants.ITEM_NAMESPACE_URI,
////              "Mime","image/jpeg");
//
//
////
////        xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,itemPath1 ,XMPConst.NS_RDF,
////                XMPConst.RDF_TYPE,XmpConstants.ITEM_NAMESPACE_URI,structOptionstrue);
//
////      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,itemPath1 ,XMPConst.NS_RDF,
////              XMPConst.RDF_TYPE,null,structOptionstrue);
////      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,item1RdfType,XMPConst.NS_RDF,
////              "resource",XmpConstants.ITEM_NAMESPACE_URI);
//
////      //insert video info
////      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,itemPath2 ,XMPConst.NS_RDF,
////              "value",null,structOptionstrue);
////      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,item2RdfValue,XmpConstants.ITEM_NAMESPACE_URI,
////              "Semantic","MotionPhoto");
////      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,item2RdfValue,XmpConstants.ITEM_NAMESPACE_URI,
////              "Mime","video/mp4");
////      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,item2RdfValue,XmpConstants.ITEM_NAMESPACE_URI,
////              "Length","" + motionPhotoVideoLength);
////
////      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,itemPath2 ,XMPConst.NS_RDF,
////              XMPConst.RDF_TYPE,null,structOptionstrue);
////      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,item2RdfType,XMPConst.NS_RDF,
////              "resource",XmpConstants.ITEM_NAMESPACE_URI);


//      006
      PropertyOptions arrayOptions = new PropertyOptions();
      arrayOptions.setArray(true);
      arrayOptions.setArrayOrdered(true);
      PropertyOptions structOptionstrue = new PropertyOptions();
      structOptionstrue.setStruct(true);

      PropertyOptions structOptionsfalse = new PropertyOptions();
      structOptionsfalse.setStruct(false);

      // array
      xmpMeta.appendArrayItem(XmpConstants.CONTAINER_NAMESPACE_URI,"Directory",arrayOptions,null,structOptionstrue);
      xmpMeta.appendArrayItem(XmpConstants.CONTAINER_NAMESPACE_URI,"Directory",arrayOptions,null,structOptionstrue);


      // path
      String itemPath1 = XMPPathFactory.composeArrayItemPath("Directory",1);
      String itemPath2 = XMPPathFactory.composeArrayItemPath("Directory",2);

      String structFieldContainer = XMPPathFactory.composeStructFieldPath(XmpConstants.CONTAINER_NAMESPACE_URI,"Item");
      String structFieldContainer1 = itemPath1 + structFieldContainer;
      String structFieldContainer2 = itemPath2 + structFieldContainer;

      //insert primary pic info
      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,itemPath1 ,XmpConstants.CONTAINER_NAMESPACE_URI,
              "Item",null,structOptionstrue);
//      xmpMeta.setProperty(XmpConstants.CONTAINER_NAMESPACE_URI,tempProper,"Primary");
      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,structFieldContainer1,XmpConstants.ITEM_NAMESPACE_URI,
              "Semantic","Primary");
      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,structFieldContainer1,XmpConstants.ITEM_NAMESPACE_URI,
              "Mime","image/jpeg");
      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,structFieldContainer1,XmpConstants.ITEM_NAMESPACE_URI,
              "Padding","" + motionPhotoPaddingLength);

      //insert video info
      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,itemPath2,XmpConstants.CONTAINER_NAMESPACE_URI,
                "Item",null,structOptionstrue);
      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,structFieldContainer2,XmpConstants.ITEM_NAMESPACE_URI,
              "Semantic","MotionPhoto");
      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,structFieldContainer2,XmpConstants.ITEM_NAMESPACE_URI,
              "Mime","video/mp4");
      xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,structFieldContainer2,XmpConstants.ITEM_NAMESPACE_URI,
              "Length","" + motionPhotoVideoLength);
    }
    return xmpMeta;
  }

  public String changToString(XMPMeta meta) {
    String result;
    try {
      SerializeOptions options = new SerializeOptions();
      options.setUseCompactFormat(true);
      // We have to omit packet wrapper here because
      // javax.xml.parsers.DocumentBuilder
      // fails to parse the packet end <?xpacket end="w"?> in android.
      options.setOmitPacketWrapper(true);
      result = XMPMetaFactory.serializeToString(meta,options);
    } catch (XMPException e) {
     // Log.d(TAG, "Serialize xmp failed", e);
      return null;
    }
    return result;
  }

    public XMPMeta buildMotion(int videoLength) throws XMPException {
        XMPMeta xmpMeta = XMPMetaFactory.create();
        XMPMetaFactory.getSchemaRegistry()
                .registerNamespace(XmpConstants.CONTAINER_NAMESPACE_URI, XmpConstants.CONTAINER_NAMESPACE_PREFIX);
        XMPMetaFactory.getSchemaRegistry()
                .registerNamespace(XmpConstants.ITEM_NAMESPACE_URI, XmpConstants.ITEM_NAMESPACE_PREFIX);
        XMPMetaFactory.getSchemaRegistry()
                .registerNamespace(XmpConstants.NAMESPACE_URI, XmpConstants.CAMERA_NAMESPACE_PREFIX);
        PropertyOptions arrayOptions = new PropertyOptions();
        arrayOptions.setArray(true);
        arrayOptions.setArrayOrdered(true);
        PropertyOptions structOptions = new PropertyOptions();
        structOptions.setStruct(true);

        // array
        xmpMeta.appendArrayItem(XmpConstants.CONTAINER_NAMESPACE_URI,"Directory",arrayOptions,null,structOptions);

        //insert primary pic info
        String itemPath1 = XMPPathFactory.composeArrayItemPath("Directory",1);
        xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,itemPath1,XmpConstants.ITEM_NAMESPACE_URI,
                "Semantic","Primary",structOptions);
        xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,itemPath1,XmpConstants.ITEM_NAMESPACE_URI,
                "Mime","image/jpeg",structOptions);

        //insert video info
        String itemPath2 = XMPPathFactory.composeArrayItemPath("Directory",2);
        xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,itemPath2,XmpConstants.ITEM_NAMESPACE_URI,
                "Semantic","MotionPhoto",structOptions);
        xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,itemPath2,XmpConstants.ITEM_NAMESPACE_URI,
                "Mime","video/mp4",structOptions);
        xmpMeta.setStructField(XmpConstants.CONTAINER_NAMESPACE_URI,itemPath2,XmpConstants.ITEM_NAMESPACE_URI,
                "Length","" + videoLength,structOptions);

//
//
//        xmpMeta.appendArrayItem(XmpConstants.CONTAINER_NAMESPACE_URI,"Container:Directory",arrayOptions,null,structOptions);
//        xmpMeta.setProperty(XmpConstants.CONTAINER_NAMESPACE_URI,"Container:Directory[1]/Item:Semantic","Primary");
//        xmpMeta.setProperty(XmpConstants.CONTAINER_NAMESPACE_URI,"Container:Directory[1]/Item:Mime","image/jpeg");
//
//
//        xmpMeta.appendArrayItem(XmpConstants.CONTAINER_NAMESPACE_URI,"Container:Directory",arrayOptions,null,structOptions);
//        xmpMeta.setProperty(XmpConstants.CONTAINER_NAMESPACE_URI,"Container:Directory[2]/Item:Semantic","MotionPhoto");
//        xmpMeta.setProperty(XmpConstants.CONTAINER_NAMESPACE_URI,"Container:Directory[2]/Item:Mime","video/mp4");
//        xmpMeta.setProperty(XmpConstants.CONTAINER_NAMESPACE_URI,"Container:Directory[2]/Item:Length",videoLength);

        //insert other info
        xmpMeta.setProperty(XmpConstants.NAMESPACE_URI,XmpConstants.PROPERTY_MOTIONPHOTO,1);
        xmpMeta.setProperty(XmpConstants.NAMESPACE_URI,XmpConstants.PROPERTY_MOTIONPHOTO_VERSION,1);
        xmpMeta.setProperty(XmpConstants.NAMESPACE_URI,XmpConstants.PROPERTY_MOTIONPHOTO_PRESENTATION_TIMESTAMP_US,500000);
        return xmpMeta;
    }

//  public XMPMeta buildMotion(int videoLength) throws XMPException {
//    XMPMeta xmpMeta = XMPMetaFactory.create();
//    XMPMetaFactory.getSchemaRegistry()
//            .registerNamespace(XmpConstants.CONTAINER_NAMESPACE_URI, XmpConstants.CONTAINER_NAMESPACE_PREFIX);
//    XMPMetaFactory.getSchemaRegistry()
//            .registerNamespace(XmpConstants.ITEM_NAMESPACE_URI, XmpConstants.ITEM_NAMESPACE_PREFIX);
//    XMPMetaFactory.getSchemaRegistry()
//            .registerNamespace(XmpConstants.NAMESPACE_URI, XmpConstants.CAMERA_NAMESPACE_PREFIX);
//    PropertyOptions arrayOptions = new PropertyOptions();
//    arrayOptions.setArray(true);
//    arrayOptions.setArrayOrdered(true);
//    PropertyOptions structOptions = new PropertyOptions();
//    structOptions.setStruct(true);
//    //insert primary pic info
//    xmpMeta.appendArrayItem(XmpConstants.CONTAINER_NAMESPACE_URI,"Container:Directory",arrayOptions,null,structOptions);
//    xmpMeta.setProperty(XmpConstants.CONTAINER_NAMESPACE_URI,"Container:Directory[1]/Item:Semantic","Primary");
//    xmpMeta.setProperty(XmpConstants.CONTAINER_NAMESPACE_URI,"Container:Directory[1]/Item:Mime","image/jpeg");
//
//    //insert video info
//    xmpMeta.appendArrayItem(XmpConstants.CONTAINER_NAMESPACE_URI,"Container:Directory",arrayOptions,null,structOptions);
//    xmpMeta.setProperty(XmpConstants.CONTAINER_NAMESPACE_URI,"Container:Directory[2]/Item:Semantic","MotionPhoto");
//    xmpMeta.setProperty(XmpConstants.CONTAINER_NAMESPACE_URI,"Container:Directory[2]/Item:Mime","video/mp4");
//    xmpMeta.setProperty(XmpConstants.CONTAINER_NAMESPACE_URI,"Container:Directory[2]/Item:Length",videoLength);
//
//    //insert other info
//    xmpMeta.setProperty(XmpConstants.NAMESPACE_URI,XmpConstants.PROPERTY_MOTIONPHOTO,1);
//    xmpMeta.setProperty(XmpConstants.NAMESPACE_URI,XmpConstants.PROPERTY_MOTIONPHOTO_VERSION,1);
//    xmpMeta.setProperty(XmpConstants.NAMESPACE_URI,XmpConstants.PROPERTY_MOTIONPHOTO_PRESENTATION_TIMESTAMP_US,500000);
//    return xmpMeta;
//  }

  private static void setProperty(XMPMeta xmpMeta, String name, String value) throws XMPException {
    PropertyOptions propertyOptions = new PropertyOptions();
    propertyOptions.setHasQualifiers(true);
    xmpMeta.setProperty(XmpConstants.NAMESPACE_URI, name, value, propertyOptions);
  }

  String prettyPrint(String prefix) {
    StringBuilder sb = new StringBuilder();
    if (specialTypeId != null) {
      sb.append(prefix)
          .append(XmpConstants.PROPERTY_SPECIAL_TYPE_ID)
          .append(": ")
          .append(specialTypeId)
          .append('\n');
    }
    if (burstId != null) {
      sb.append(prefix)
          .append(XmpConstants.PROPERTY_BURST_ID)
          .append(": ")
          .append(burstId)
          .append('\n');
    }
    if (isPrimaryInBurst != null) {
      sb.append(prefix)
          .append(XmpConstants.PROPERTY_BURST_PRIMARY)
          .append(": ")
          .append(isPrimaryInBurst ? 1 : 0)
          .append('\n');
    }
    if (disabledAutoCreations != null) {
      String[] autoCreationValues = new String[disabledAutoCreations.size()];
      int i = 0;
      for (AutoCreation autoCreation : disabledAutoCreations) {
        autoCreationValues[i] = autoCreation.getValue();
        i++;
      }

      sb.append(prefix)
          .append(XmpConstants.PROPERTY_DISABLE_AUTO_CREATION)
          .append(": ")
          .append(Arrays.toString(autoCreationValues))
          .append('\n');
    }

    return sb.toString();
  }

  @Override
  public String toString() {
    return "XmpBuilder{" +
        "specialTypeId='" + specialTypeId + '\'' +
        ", burstId='" + burstId + '\'' +
        ", isPrimaryInBurst=" + isPrimaryInBurst +
        ", disabledAutoCreations=" + disabledAutoCreations +
        '}';
  }
}
