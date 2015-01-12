#! /usr/bin/perl
# IBM_PROLOG_BEGIN_TAG
# This is an automatically generated prolog.
#
# $Source: src/usr/targeting/common/prettyPerlDoc.pl $
#
# OpenPOWER HostBoot Project
#
# Contributors Listed Below - COPYRIGHT 2015
# [+] International Business Machines Corp.
#
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied. See the License for the specific language governing
# permissions and limitations under the License.
#
# IBM_PROLOG_END_TAG

use Pod::HtmlEasy;

if ($ARGV[0] eq "") {
    die "Usage:  prettyPerlDoc.pl [.pm filename]\n";
}

my $outfile=$ARGV[0].".html";
my $podhtml = Pod::HtmlEasy->new ();
my $html = $podhtml->pod2html($ARGV[0] );
open(OUT,">$outfile") || die "Unable to Targets.html\n";
print OUT $html;
close OUT;
print "Done\n";
