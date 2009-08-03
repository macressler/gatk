package org.broadinstitute.sting.playground.gatk.walkers.variants;

import org.broadinstitute.sting.gatk.refdata.rodVariants;
import org.broadinstitute.sting.gatk.LocusContext;
import org.broadinstitute.sting.utils.*;

public class VECOnOffGenotypeRatio implements VariantExclusionCriterion { // extends RatioFilter {
    //final private static GenotypeFeatureData.Tail tail = GenotypeFeatureData.Tail.LeftTailed;
    private boolean exclude;
    private double lowThreshold, highThreshold, ratio;

    public void initialize(String arguments) {
        if (arguments != null && !arguments.isEmpty()) {
            String[] argPieces = arguments.split(",");
            lowThreshold = Double.valueOf(argPieces[0]);
            highThreshold = Double.valueOf(argPieces[1]);
        }
    }

    /**
     * On/off genotype filters can be applied to any genotype
     *
     * @param variant
     * @return
     */
    protected boolean applyToVariant(rodVariants variant) {
        return true;
    }

    /**
     * Return the counts of bases that are on (matching the bestGenotype) and off (not matching the
     * best genotype).  On are in the first field, off in the second.
     *
     * @param ref
     * @param pileup
     * @param variant
     * @return
     */
    protected Pair<Integer, Integer> scoreVariant(char ref, ReadBackedPileup pileup, rodVariants variant) {
        final String genotype = variant.getBestGenotype().toUpperCase();
        final String bases = pileup.getBases();

        if ( genotype.length() > 2 )
            throw new IllegalArgumentException(String.format("Can only handle diploid genotypes: %s", genotype));

        int on = 0, off = 0;

        for ( char base : BaseUtils.BASES ) {
            int count = BasicPileup.countBase(base, bases);
            if ( Utils.countOccurrences(base, genotype) > 0 )
                on += count;
            else
                off += count;
            //System.out.printf("count = %d, on=%d, off=%d for %c in %s%n", count, on, off, base, genotype);            
        }

        return new Pair<Integer, Integer>(on, off);
    }

    public void compute(char ref, LocusContext context, rodVariants variant) {
        ReadBackedPileup pileup = new ReadBackedPileup(ref, context);
        Pair<Integer, Integer> counts = scoreVariant(ref, pileup, variant);
        ratio = (double)counts.first / (double)counts.second;
        exclude = ratio < lowThreshold || ratio > highThreshold;
    }

    public boolean useZeroQualityReads() { return false; }

    public boolean isExcludable() {
        return exclude;
    }

    public String getStudyHeader() {
        return "OnOffGenotype("+lowThreshold+","+highThreshold+")\tMajorMinorRatio";
    }

    public String getStudyInfo() {
        return (exclude ? "fail" : "pass") + "\t" + ratio;
    }
}