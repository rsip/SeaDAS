package org.esa.beam.framework.gpf.descriptor;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

/**
 * A {@link TargetProductDescriptor} implementation for the
 * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class AnnotationTargetProductDescriptor implements TargetProductDescriptor {

    private final String name;
    private final TargetProduct annotation;

    public AnnotationTargetProductDescriptor(String name, TargetProduct annotation) {
        Assert.notNull(name, "name");
        Assert.notNull(annotation, "annotation");
        this.name = name;
        this.annotation = annotation;
    }

    @Override
    public String getName() {
        return name;
    }

    public TargetProduct getAnnotation() {
        return annotation;
    }

    @Override
    public String getAlias() {
        return "target";
    }

    @Override
    public String getLabel() {
        return getNonEmptyStringOrNull(annotation.label());
    }

    @Override
    public String getDescription() {
        return getNonEmptyStringOrNull(annotation.description());
    }

    @Override
    public Class<? extends Product> getDataType() {
        return Product.class;
    }

    private static String getNonEmptyStringOrNull(String label) {
        return label == null || label.isEmpty() ? null : label;
    }
}
