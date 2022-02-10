package  com.android.camera.util;

/**
 * Constants used to write special type XMP.
 */
public final class XmpConstants {
  private XmpConstants() { }

  /**
   * The OEM creations namespace url.
   */
  static final String NAMESPACE_URI = "http://ns.google.com/photos/1.0/camera/";

  /*this is for motion photos*/
  static final String CONTAINER_NAMESPACE_URI = "http://ns.google.com/photos/1.0/container/";
  static final String ITEM_NAMESPACE_URI = "http://ns.google.com/photos/1.0/container/item/";
  static final String CAMERA_NAMESPACE_PREFIX = "Camera";
  static final String CONTAINER_NAMESPACE_PREFIX = "Container";
  static final String ITEM_NAMESPACE_PREFIX = "Item";
  static final String PROPERTY_DIRECTORY = "Directory";
  static final String PROPERTY_MIME = "Mime";
  static final String PROPERTY_SEMANTIC = "Semantic";
  static final String PROPERTY_LENGTH = "Length";
  static final String PROPERTY_PADDING = "Padding";
  static final String PROPERTY_MOTIONPHOTO = "MotionPhoto";
  static final String PROPERTY_MOTIONPHOTO_VERSION = "MotionPhotoVersion";
  static final String PROPERTY_MOTIONPHOTO_PRESENTATION_TIMESTAMP_US = "MotionPhotoPresentationTimestampUs";

  /**
   * The namespace prefix for the properties listed below.
   */
  static final String NAMESPACE_PREFIX = "GCamera";

  /**
   * The property containing the opaque id string created by the OEM.
   *
   * <p>For bursts, this field should not be present. Instead, the two properties below will allow
   * Photos to identify and provide special treatment for bursts.
   */
  static final String PROPERTY_SPECIAL_TYPE_ID = "SpecialTypeID";
  /**
   * The property for identifying a burst that one or more photos belong to consisting of a unique
   * String.
   *
   * <p>The property should be present and identical for all images that make up a burst. It should
   * be unique across devices (UUID recommended).
   *
   * Unlike GCreations:BurstID, we should use images with this property to create auto collages and
   * animations (except those with the DisableAutoCreation property below).
   */
  static final String PROPERTY_BURST_ID = "BurstID";

  /**
   * The optional property for identifying the primary (“best shot”) image at capture time.
   *
   * <p>A value of 1 should be used to indicate that the photo containing this property is the
   * primary image in the burst.
   *
   * <p>This value is optional, cameras are not required to set it on any photo in a burst. Clients
   * will default to the 0th frame, but may run an algorithm to pick a better default.
   */
  static final String PROPERTY_BURST_PRIMARY = "BurstPrimary";

  /**
   * The optional property consistenting of an unordered array of Strings that prevents Photos from
   * using the image containing the property in any of the specified auto creations.
   *
   * <p>The possible values are: “Animation”, “Collage”, “Pano”, “Movies”. Photos will avoid
   * creating the listed types using the containing image or video.
   *
   * <p>The property is optional. The property can be included multiple times to disable creation of
   * multiple different types.
   */
  static final String PROPERTY_DISABLE_AUTO_CREATION = "DisableAutoCreation";

  /** Disables animated GIFs. */
  static final String VALUE_DISABLE_ANIMATIONS = "Animation";

  /** Disables collages. */
  static final String VALUE_DISABLE_COLLAGE = "Collage";

  /** Disables panoramas. */
  static final String VALUE_DISABLE_PANO = "Pano";

  /** Disables movies. */
  static final String VALUE_DISABLE_MOVIES = "Movies";

}
