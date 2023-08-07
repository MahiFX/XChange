package com.knowm.xchange.vertex.dto;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

public class VertexModelUtilsTest {
    @Test
    public void testNonZero() {
        Assert.assertFalse(VertexModelUtils.nonZero((BigDecimal) null));
        Assert.assertFalse(VertexModelUtils.nonZero(VertexModelUtils.convertToDecimal(null)));
        Assert.assertFalse(VertexModelUtils.nonZero(VertexModelUtils.convertToDecimal(new BigInteger("0"))));
        Assert.assertTrue(VertexModelUtils.nonZero(VertexModelUtils.convertToDecimal(new BigInteger("1"))));

        Assert.assertFalse(VertexModelUtils.nonZero((BigInteger) null));
        Assert.assertFalse(VertexModelUtils.nonZero(new BigInteger("0")));
        Assert.assertTrue(VertexModelUtils.nonZero(new BigInteger("1")));
    }
}