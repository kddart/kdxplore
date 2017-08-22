library('getopt');
library('agricolaeTrialDesign')


spec = matrix(c(
    'outputdir'		, 'out', 0, "character",
    'rns'			, 's', 0, "integer",
    'treatmentfile', 'tf',  0, "character",

  'numrows',  'nr', 0, "integer",
  'numcols',  'nc', 0, "integer",
  'sizeofblock'		, 'nb',  0, "integer"

), byrow=TRUE, ncol=4);
opt = getopt(spec);
print(opt)

if(! is.null(opt$rns)) {
	rns = opt$rns;
}else{
	rns = 0;
}

trt = as.list(read.csv(opt$treatmentfile,header = T))$Treatment;
trialDesignBib(trt,opt$outputdir,opt$sizeofblock,rns,opt$numrows,opt$numcols)
