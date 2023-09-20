<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		<xsl:text>caseJudgements: "</xsl:text>
		<xsl:value-of select="normalize-space(query/userInput)" />
		<xsl:text>"</xsl:text>
	</xsl:template>
</xsl:stylesheet>