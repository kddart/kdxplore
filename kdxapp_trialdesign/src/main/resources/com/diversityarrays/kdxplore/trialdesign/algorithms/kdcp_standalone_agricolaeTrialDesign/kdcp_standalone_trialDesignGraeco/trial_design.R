library('getopt');
library('agricolaeTrialDesign')


spec = matrix(c(
    'outputdir'		, 'out', 0, "character",
    'rns'			, 's', 0, "integer",
    'treatmentfile', 'tf',  0, "character"
), byrow=TRUE, ncol=4);
opt = getopt(spec);
print(opt)

if(! is.null(opt$rns)) {
	rns = opt$rns;
}else{
	rns = 0;
}

trt = read.csv(opt$treatmentfile,header = T);
trialDesignGraeco(trt$Treatment1,trt$Treatment2,opt$outputdir,rns)
