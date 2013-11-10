package com.google.collide.client.util;

import javax.inject.Provider;

import collide.client.util.CssUtils;

import com.google.collide.client.ui.panel.Panel.Interpolator;

import elemental.client.Browser;
import elemental.dom.Element;

public class ResizeBounds {

  private Provider<Float> 
  minWidth, maxWidth
  , minHeight, maxHeight
  , prefHeight, prefWidth
  ;

  private static class ImmutableProvider implements Provider<Float> {
    private float value;

    public ImmutableProvider(float value) {
      this.value = value;
    }
    @Override
    public Float get() {
      return value;
    }
  }
  private static abstract class ElementalProvider implements Provider<Float> {
    private final Element element;
    private final Interpolator interpolator;

    public ElementalProvider(Element element, Interpolator interpolator) {
      this.element = element;
      this.interpolator = interpolator == null ? Interpolator.NO_OP : interpolator;
    }
    @Override
    public Float get() {
      return getValue(element, interpolator);
    }
    protected abstract Float getValue(Element element, Interpolator interpolator);
  }
  private static class ElementalHeightProvider extends ElementalProvider {

    public ElementalHeightProvider(Element element, Interpolator interpolator) {
      super(element, interpolator);
    }

    @Override
    protected Float getValue(Element element, Interpolator interpolator) {
      return interpolator.interpolate(CssUtils.parsePixels(element.getStyle().getHeight()));
    }
  }
  private static class ElementalWidthProvider extends ElementalProvider {

    public ElementalWidthProvider(Element element, Interpolator interpolator) {
      super(element, interpolator);
    }

    @Override
    protected Float getValue(Element element, Interpolator interpolator) {
      // we want to use the css width, and not the element's actual width,
      // in case it has padding or borders we want to ignore.
      return interpolator.interpolate(CssUtils.parsePixels(element.getStyle().getWidth()));
    }
  }

  /**
   * @return the minHeight
   */
  public float getMinHeight() {
    Float min = minHeight.get();
    if (min < 0)
      min = Browser.getWindow().getInnerHeight()+min;
    return min;
  }
  /**
   * @return the maxHeight
   */
  public float getMaxHeight() {
    Float max = maxHeight.get();
    if (max < 0)
      max = Browser.getWindow().getInnerHeight()+max;
    return max;
  }
  /**
   * @return the minWidth
   */
  public float getMinWidth() {
    Float min = minWidth.get();
    if (min < 0)
      min = Browser.getWindow().getInnerWidth()+min;
    return min;
  }
  /**
   * @return the maxWidth
   */
  public float getMaxWidth() {
    Float max = maxWidth.get();
    if (max < 0)
      max = Browser.getWindow().getInnerWidth()+max;
    return max;
  }

  public static BoundsBuilder withMaxSize(float maxWidth, float maxHeight) {
    BoundsBuilder builder = new BoundsBuilder();
    builder.maxWidth = maxWidth;
    builder.maxHeight = maxHeight;
    return builder;
  }
  public static BoundsBuilder withMinSize(float minWidth, float minHeight) {
    BoundsBuilder builder = new BoundsBuilder();
    builder.minWidth = minWidth;
    builder.minHeight = minHeight;
    return builder;
  }
  public static BoundsBuilder withPrefSize(float minWidth, float minHeight) {
    BoundsBuilder builder = new BoundsBuilder();
    builder.prefWidth = minWidth;
    builder.prefHeight = minHeight;
    return builder;
  }

  public static class BoundsBuilder {
    private float
      minHeight=Integer.MIN_VALUE
      , minWidth=Integer.MIN_VALUE
      , maxWidth=Integer.MAX_VALUE
      , maxHeight=Integer.MAX_VALUE
      , prefWidth=-10
      , prefHeight=-10
      ;
    private Provider<Float>
      minWidthProvider, maxWidthProvider, 
      minHeightProvider, maxHeightProvider,
      prefHeightProvider, prefWidthProvider
      ;

    public BoundsBuilder maxSize(float maxWidth, float maxHeight) {
      this.maxWidth = maxWidth;
      this.maxHeight = maxHeight;
      return this;
    }
    public BoundsBuilder prefSize(float prefWidth, float prefHeight) {
      this.prefWidth = prefWidth;
      this.prefHeight = prefHeight;
      return this;
    }
    public BoundsBuilder minSize(float minWidth, float minHeight) {
      this.minWidth = minWidth;
      this.minHeight = minHeight;
      return this;
    }
    
    public BoundsBuilder minWidth(Provider<Float> minWidth) {
      minWidthProvider = minWidth;
      return this;
    }

    public BoundsBuilder minWidth(float minWidth) {
      minWidthProvider = null;
      this.minWidth = minWidth;
      return this;
    }
    
    public BoundsBuilder minHeight(Provider<Float> minHeight) {
      minHeightProvider = minHeight;
      return this;
    }
    
    public BoundsBuilder minHeight(float minHeight) {
      minHeightProvider = null;
      this.minHeight = minHeight;
      return this;
    }
    
    public BoundsBuilder maxWidth(Provider<Float> maxWidth) {
      maxWidthProvider = maxWidth;
      return this;
    }
    
    public BoundsBuilder maxWidth(float maxWidth) {
      maxWidthProvider = null;
      this.maxWidth = maxWidth;
      return this;
    }
    
    public BoundsBuilder maxHeight(Provider<Float> maxHeight) {
      maxHeightProvider = maxHeight;
      return this;
    }
    
    public BoundsBuilder maxHeight(float maxHeight) {
      maxHeightProvider = null;
      this.maxHeight = maxHeight;
      return this;
    }
    
    public BoundsBuilder prefWidth(Provider<Float> prefWidth) {
      prefWidthProvider = prefWidth;
      return this;
    }
    
    public BoundsBuilder prefWidth(float prefWidth) {
      prefWidthProvider = null;
      this.prefWidth = prefWidth;
      return this;
    }
    
    public BoundsBuilder prefHeight(Provider<Float> prefHeight) {
      prefHeightProvider = prefHeight;
      return this;
    }
    
    public BoundsBuilder prefHeight(float prefHeight) {
      prefHeightProvider = null;
      this.prefHeight = prefHeight;
      return this;
    }

    public ResizeBounds build() {
      ResizeBounds bounds = new ResizeBounds();

      if (minHeightProvider == null)
        bounds.minHeight = new ImmutableProvider(minHeight);
      else
        bounds.minHeight = minHeightProvider;

      if (minWidthProvider == null)
        bounds.minWidth = new ImmutableProvider(minWidth);
      else
        bounds.minWidth = minWidthProvider;

      if (maxHeightProvider == null)
        bounds.maxHeight = new ImmutableProvider(maxHeight);
      else
        bounds.maxHeight = maxHeightProvider;

      if (maxWidthProvider == null)
        bounds.maxWidth = new ImmutableProvider(maxWidth);
      else
        bounds.maxWidth = maxWidthProvider;
      
      if (prefHeightProvider == null) {
        bounds.prefHeight = new ImmutableProvider(prefHeight);
      }
      else
        bounds.prefHeight = prefHeightProvider;
      
      if (prefWidthProvider == null)
        bounds.prefWidth = new ImmutableProvider(prefWidth);
      else
        bounds.prefWidth = prefWidthProvider;
      
      return bounds;
    }
    public BoundsBuilder maxHeight(Element body, Interpolator inter) {
      maxHeightProvider = new ElementalHeightProvider(body, inter);
      return this;
    }
    public BoundsBuilder maxWidth(Element body, Interpolator inter) {
      maxWidthProvider = new ElementalWidthProvider(body, inter);
      return this;
    }
    public BoundsBuilder minHeight(Element body, Interpolator inter) {
      minHeightProvider = new ElementalHeightProvider(body, inter);
      return this;
    }
    public BoundsBuilder minWidth(Element body, Interpolator inter) {
      minWidthProvider = new ElementalWidthProvider(body, inter);
      return this;
    }
  }

  public float getPreferredWidth() {
    Float pref = prefWidth.get();
    if (pref < 0)
      pref = getMaxWidth() + pref;
    return pref;
  }
  public float getPreferredHeight() {
    Float pref = prefHeight.get();
    if (pref < 0)
      pref = getMaxHeight() + pref;
    return pref;
  }

}
