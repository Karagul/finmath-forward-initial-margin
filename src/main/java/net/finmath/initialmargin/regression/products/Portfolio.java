/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 15.02.2004
 */
package net.finmath.initialmargin.regression.products;

import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.initialmargin.regression.products.components.AbstractProductComponent;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements the pricing of a portfolio of AbstractLIBORMonteCarloProduct products
 * under a AbstractLIBORMarketModel. The products can be scaled by weights.
 * <p>
 * The value of the portfolio is that of
 * \( \sum_{i=0}^{n} weights\[i\] \cdot products\[i\] \text{.} \)
 * <p>
 * Note: Currently the products have to be of the same currency.
 *
 * @author Christian Fries
 * @version 1.2
 * @date 08.09.2006
 */
public class Portfolio extends AbstractProductComponent {

	private static final long serialVersionUID = -1360506093081238482L;

	private AbstractLIBORMonteCarloRegressionProduct[] products;
	private double[] weights;
	private double initialLifeTime;

	/**
	 * Creates a portfolio consisting of a single product and a weight.
	 * <p>
	 * The currency of this portfolio is the currency of the product.
	 *
	 * @param product A product.
	 * @param weight  A weight.
	 */
	public Portfolio(AbstractLIBORMonteCarloRegressionProduct product, double weight) {
		super(product.getCurrency());
		this.products = new AbstractLIBORMonteCarloRegressionProduct[]{product};
		this.weights = new double[]{weight};
	}

	/**
	 * Creates a portfolio consisting of a set of products and a weights.
	 * <p>
	 * Note: Currently the products have to be of the same currency.
	 *
	 * @param products An array of products.
	 * @param weights  An array of weights (having the same lengths as the array of products).
	 */
	public Portfolio(AbstractLIBORMonteCarloRegressionProduct[] products, double[] weights) {
		super();
		//String currency = products[0].getCurrency();
		//for(AbstractLIBORMonteCarloProduct product : products) if(!currency.equals(product.getCurrency()))
		//	throw new IllegalArgumentException("Product currencies do not match. Please use a constructor providing the currency of the result.");

		this.products = products;
		this.weights = weights;
	}

	/**
	 * Creates a portfolio consisting of a set of products and a weights.
	 * <p>
	 * Note: Currently the products have to be of the same currency, namely the one provided.
	 *
	 * @param currency The currency of the value of this portfolio when calling <code>getValue</code>.
	 * @param products An array of products.
	 * @param weights  An array of weights (having the same lengths as the array of products).
	 */
	public Portfolio(String currency, AbstractLIBORMonteCarloRegressionProduct[] products, double[] weights) {
		super(currency);

		for (AbstractLIBORMonteCarloProduct product : products) {
			if (!currency.equals(product.getCurrency())) {
				throw new IllegalArgumentException("Product currencies do not match. Currency conversion (via model FX) is not supported yet.");
			}
		}

		this.products = products;
		this.weights = weights;
	}

	@Override
	public String getCurrency() {
		// @TODO: We report only the currency of the first item, because mixed currency portfolios are currently not allowed.
		return (products != null && products.length > 0) ? products[0].getCurrency() : null;
	}

	@Override
	public Set<String> queryUnderlyings() {
		Set<String> underlyingNames = null;
		for (AbstractLIBORMonteCarloProduct product : products) {
			Set<String> productUnderlyingNames;
			if (product instanceof AbstractProductComponent) {
				productUnderlyingNames = ((AbstractProductComponent) product).queryUnderlyings();
			} else {
				throw new IllegalArgumentException("Underlying cannot be queried for underlyings.");
			}

			if (productUnderlyingNames != null) {
				if (underlyingNames == null) {
					underlyingNames = productUnderlyingNames;
				} else {
					underlyingNames.addAll(productUnderlyingNames);
				}
			}
		}
		return underlyingNames;
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model          The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 * @TODO The conversion between different currencies is currently not performed.
	 */
	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		RandomVariableInterface values = new RandomVariable(0.0);

		for (int productIndex = 0; productIndex < products.length; productIndex++) {
			RandomVariableInterface valueOfProduct = products[productIndex].getValue(evaluationTime, model);
			double weightOfProduct = weights[productIndex];

			values = valueOfProduct.mult(weightOfProduct).add(values);
		}
		return values;
	}

	@Override
	public RandomVariableInterface getCF(double initialTime, double finalTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		RandomVariableInterface cashFlows = new RandomVariable(0.0);

		for (int productIndex = 0; productIndex < products.length; productIndex++) {
			RandomVariableInterface cashFlowsOfProduct = products[productIndex].getCF(initialTime, finalTime, model);
			double weightOfProduct = weights[productIndex];
			cashFlows = cashFlowsOfProduct.mult(weightOfProduct).add(cashFlows);
		}
		return cashFlows;
	}

	/**
	 * @return the products
	 */
	public AbstractLIBORMonteCarloProduct[] getProducts() {
		return products.clone();
	}

	/**
	 * @return the weights
	 */
	public double[] getWeights() {
		return weights.clone();
	}

	public double getInitialLifeTime() {
		return initialLifeTime;
	}

	public void setInitialLifeTime(double initialLifeTime) {
		this.initialLifeTime = initialLifeTime;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, double fixingDate,
			LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		// TODO Auto-generated method stub
		return null;
	}
}
