
<?php
	/*
		Template Name: Custom Data Page Template
	*/
	
	session_start();
	get_header(); // Include the header of your theme
?>

<?php
	if (isset($_SESSION['query'])) {
		$data = $_SESSION['query']; // Retrieve the data from the session
		unset($_SESSION['query']); // Clear the session variable
		} else {
		$data = []; // Initialize an empty array if no data available
	}
?>
<div class="content custom-div">
	<?php if (!empty($data)): ?>
	<h2>Search Results</h2>
	<table>
		<tr>
		<!--	<th>Document ID</th> -->
		<!--	<th>File Name</th> -->
			<th>Jurisdiction</th>
			<th>Case Title</th>
			<th>Case Author</th>
			<th>Case Jurisdiction</th>
			<th>Case Judges</th>
		</tr>
		<?php foreach ($data as $document): ?>
		<tr>
			<!--  <td><?php echo $document['Document ID']; ?></td> -->
			<!-- <td><?php echo $document['Fields']['fileName']; ?></td> -->
			<td><?php echo $document['Fields']['jurisdiction']; ?></td>
			<td><?php echo $document['Fields']['caseTitle']; ?></td>
			<td><?php echo $document['Fields']['caseAuthor']; ?></td>
			<td><?php echo $document['Fields']['caseJurisdiction']; ?></td>
			<td><?php echo $document['Fields']['caseJudges']; ?></td>
		</tr>
		<?php endforeach; ?>
	</table>
	<?php else: ?>
	<p>No data available.</p>
	<?php endif; ?>
</div>

<?php
	get_footer(); // Include the footer of your theme
?>

